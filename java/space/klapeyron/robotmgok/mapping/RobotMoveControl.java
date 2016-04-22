package space.klapeyron.robotmgok.mapping;

import android.util.Log;

import ru.rbot.android.bridge.service.robotcontroll.controllers.BodyController;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.TwoWheelsBodyController;
import ru.rbot.android.bridge.service.robotcontroll.exceptions.ControllerException;
import space.klapeyron.robotmgok.RobotWrap;

public class RobotMoveControl {
    private RobotWrap robotWrap;
    private TwoWheelsBodyController wheelsController;

    public RobotMoveControl(RobotWrap robotWrap) {
        this.robotWrap = robotWrap;
    }

    public void turnLeft() {
        Log.i("TAG", "turnLeft Start");
                AngleTurnThreadSimple angleTurnThreadSimple = new AngleTurnThreadSimple((float)Math.PI/2);
                angleTurnThreadSimple.start();
                try {
                    angleTurnThreadSimple.join();
                } catch (InterruptedException e) {}
        Log.i("TAG", "turnLeft Stop");
    }

    public void turnRight() {
        Log.i("TAG", "turnRight Start");
                AngleTurnThreadSimple angleTurnThreadSimple = new AngleTurnThreadSimple(-(float) Math.PI / 2);
                angleTurnThreadSimple.start();
                try {
                    angleTurnThreadSimple.join();
                } catch (InterruptedException e) {}
        Log.i("TAG", "turnRight Stop");
    }

    public void moveForward() {
        Log.i("TAG", "forwardStart");
                ForwardMoveThread forwardMoveThread = new ForwardMoveThread();
                forwardMoveThread.start();
                try {
                    forwardMoveThread.join();
                } catch (InterruptedException e) {}
        Log.i("TAG", "forwardStop");
    }

    private class AngleTurnThreadSimple extends Thread {
        private float purposeAngle;
        private float startAngle;
        private float turnSpeed = 8.60f; //четыре поворота на пи/2, значение скоростей для точного 2пи поворота:
                                         //8.60-сразу после прямой езды(колесико смотрит вдоль направления робота)
                                         //8.68-после поворота(колесико смотрит перпендикулярно направлению робота)

        private float speedBuffer = 0; //если не увеличивается за dt, значит скорости на колесах - 0, буфер общий, на оба колеса.

        /**@param purposeAngle more 0 - turn left; less 0 - turn right.*/
        public AngleTurnThreadSimple(float purposeAngle) {
            this.purposeAngle = purposeAngle;
        }

        @Override
        public void run() {
            //        Log.i("TAG","turn started: "+robotWrapMapping.odometryAngle+";   purpose angle: ");
            startAngle = robotWrap.odometryAngle;
            if( robotWrap.robot.isControllerAvailable( BodyController.class ) ) {
                try {
                    BodyController bodyController = (BodyController) robotWrap.robot.getController( BodyController.class );
                    if( bodyController.isControllerAvailable( TwoWheelsBodyController.class ) ) {
                        wheelsController = (TwoWheelsBodyController) bodyController.getController( TwoWheelsBodyController.class );
                        wheelsController.turnAround(turnSpeed, purposeAngle);

                        CheckWheelsSpeedThread checkWheelsSpeedThread = new CheckWheelsSpeedThread();

                        while(!checkWheelsSpeedThread.getRobotStopped()) {
                            if (!checkWheelsSpeedThread.isAlive())
                                checkWheelsSpeedThread.start();
                            speedBuffer += Math.abs(robotWrap.odometryWheelSpeedLeft)+Math.abs(robotWrap.odometryWheelSpeedRight);
                        }
                        //       Log.i("TAG","turn ended: "+robotWrapMapping.odometryAngle+";   angle difference: "+differenceAngle());//Math.abs(robotWrapMapping.odometryAngle - startAngle));
                        Log.i("TAG", "speedBuffer " + speedBuffer);
                    }
                } catch (ControllerException e) {e.printStackTrace();}
            }
        }

        private class CheckWheelsSpeedThread extends Thread {
            private int dt = 500;
            private boolean robotStopped = false; //true, если робот остановился

            private float speedBufferX;

            @Override
            public void run() {
                while(!robotStopped) {
                    speedBufferX = speedBuffer;
                    try {
                        sleep(dt);
                    } catch (InterruptedException e) {
                    }
                    if (speedBufferX == speedBuffer)
                        robotStopped = true;
                }
            }

            public boolean getRobotStopped() {
                return robotStopped;
            }
        }

        /**возвращает модуль разницы между стартовым и текущим углами (для всех случаев работает)*/
        private float differenceAngle() {
            if (Math.abs(robotWrap.odometryAngle - startAngle) < Math.PI)
                return Math.abs(robotWrap.odometryAngle - startAngle);
            else
                return (float)Math.abs(Math.abs(robotWrap.odometryAngle - startAngle)-2*Math.PI);
        }
    }

    private class ForwardMoveThread extends Thread {
        private float purposeDistance = 0.5f;
        private float startDistance;
        private float startAngle;

        private float rangeValidDeviation = 0.01f;
        private float standardSpeed = 3.5f;
        private float correctionSpeed = 0.2f;
        private float correctionSpeedCorrection = 0.1f;

        public ForwardMoveThread() {}

        @Override
        public void run() {
            startDistance = robotWrap.odometryPath;
            if( robotWrap.robot.isControllerAvailable( BodyController.class ) ) {
                try {
                    BodyController bodyController = (BodyController) robotWrap.robot.getController( BodyController.class );
                    if( bodyController.isControllerAvailable( TwoWheelsBodyController.class ) ) {
                        wheelsController = (TwoWheelsBodyController) bodyController.getController( TwoWheelsBodyController.class );
                        CheckDistanceThread checkDistanceThread = new CheckDistanceThread();

                        startAngle = robotWrap.odometryAngle;
                        startDistance = robotWrap.odometryPath;
                        while(!checkDistanceThread.getRobotReachedTarget()) {
                            if (!checkDistanceThread.isAlive())
                                checkDistanceThread.start();
                            correctionCode();
                            sleep(500);
                        }
                    }
                } catch (ControllerException e) {e.printStackTrace();} catch (InterruptedException e) {}
            }
        }

        private void correctionCode() {
            float absAngleDifference;
            float angleDifference = startAngle - robotWrap.odometryAngle;

            boolean piBorderFlag = false; //true, когда между текущим и стратовым углом лежит пи/-пи граница

            if (Math.abs(robotWrap.odometryAngle - startAngle) < Math.PI) {
                absAngleDifference = Math.abs(robotWrap.odometryAngle - startAngle);
                piBorderFlag = false;
            } else {
                absAngleDifference = (float) Math.abs(Math.abs(robotWrap.odometryAngle - startAngle) - 2 * Math.PI);
                piBorderFlag = true;
            }

            if ((startAngle >= 0)&&(!piBorderFlag)) {
                //все норм
            } else
            if ((startAngle >= 0)&&(piBorderFlag)) {
                angleDifference = angleDifference - 2 * (float)Math.PI;
            } else
            if ((startAngle < 0)&&(!piBorderFlag)) {
                //все норм
            } else {
                angleDifference = angleDifference + 2 * (float)Math.PI;
            }


            if (absAngleDifference < rangeValidDeviation) {
                wheelsController.setWheelsSpeeds(standardSpeed, standardSpeed);
                Log.i("TAG","OK  ; start angle: "+startAngle+";  angle: "+ robotWrap.odometryAngle+";  difference:"+absAngleDifference+";  sign: "+angleDifference);
            } else
                if (angleDifference > 0) { //отклонение вправо, корректируем влево
                    wheelsController.setWheelsSpeeds(standardSpeed, standardSpeed);
                    Log.i("TAG", "LEFT; start angle: " + startAngle + ";  angle: " + robotWrap.odometryAngle + ";  difference:"+absAngleDifference+";  sign: "+angleDifference);
                } else { //отклонение влево, корректируем вправо
                    wheelsController.setWheelsSpeeds(standardSpeed, standardSpeed);
                    Log.i("TAG","RIGH; start angle: "+startAngle+";  angle: "+ robotWrap.odometryAngle+";  difference:"+absAngleDifference+";  sign: "+angleDifference);
                }
        }

        private class CheckDistanceThread extends Thread {
            private boolean reachedTarget = false;

            CheckDistanceThread() {}
            @Override
            public void run() {
                while(!reachedTarget) {
                    if (robotWrap.odometryPath - startDistance >= purposeDistance) {
                        reachedTarget = true;
                        Log.i("TAG","TARGET");
                    }
                }
            }
            public boolean getRobotReachedTarget() {
                return reachedTarget;
            }
        }
    }
}
