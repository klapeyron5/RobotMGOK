package space.klapeyron.robotmgok.mapping;

import android.util.Log;

import space.klapeyron.robotmgok.MainActivity;

public class BluetoothCommands {

    private RobotMoveControl robotMoveControl;

    public BluetoothCommands(MainActivity m) {
        robotMoveControl = new RobotMoveControl(m.robotWrap);
    }

    public void runFromBluetoothCommands(String key) {
        switch(key) {
            case "mapping forward":
                Log.i("TAG","mapping forward");
                moveForward();
                break;
            case "mapping half pi left":
                Log.i("TAG","mapping half pi left");
                turnLeft();
                break;
            case "mapping half pi right":
                Log.i("TAG","mapping half pi right");
                break;
            case "mapping measure":
                Log.i("TAG","mapping measure");
                break;
        }
    }

    private void moveForward() {
        robotMoveControl.moveForward();
    }

    private void turnLeft() {
        robotMoveControl.turnLeft();
    }


    private void turnRight() {
        robotMoveControl.turnRight();
    }

}
