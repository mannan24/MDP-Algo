package robot;

import map.Map;
import map.MapConstants;
import robot.RobotConstants.DIRECTION;
import robot.RobotConstants.MOVEMENT;
import utils.CommMgr;
import utils.MapDescriptor;

import java.util.concurrent.TimeUnit;

// @formatter:off
/**
 * Represents the robot moving in the arena.
 *
 * The robot is represented by a 3 x 3 cell space as below:
 *
 *          ^   ^   ^
 *         SR  SR  SR
 *       < SR
 *        [X] [X] [X]
 *   < LR [X] [X] [X] SR >
 *        [X] [X] [X]
 *
 * SR = Short Range Sensor, LR = Long Range Sensor
 *
 * @author Chio Ting Kiat
 * @author Mannan Kant */
// @formatter:on

public class Robot {
    private int posRow; // center cell
    private int posCol; // center cell
    private DIRECTION robotDir;
    private int speed;
    private final Sensor SRFrontLeft;       // north-facing front-left SR
    private final Sensor SRFrontCenter;     // north-facing front-center SR
    private final Sensor SRFrontRight;      // north-facing front-right SR
    private final Sensor SRLeft;            // west-facing left SR
    private final Sensor SRRight;           // east-facing right SR
    private final Sensor LRLeft;            // west-facing left LR
    private boolean touchedGoal;
    private final boolean realBot;

    public Robot(int row, int col, boolean realBot) {
        posRow = row;
        posCol = col;
        robotDir = RobotConstants.START_DIR;
        speed = RobotConstants.SPEED;

        this.realBot = realBot;

        SRFrontLeft = new Sensor(RobotConstants.SENSOR_SHORT_RANGE_L, RobotConstants.SENSOR_SHORT_RANGE_H, this.posRow + 1, this.posCol - 1, this.robotDir, "SRFL");
        SRFrontCenter = new Sensor(RobotConstants.SENSOR_SHORT_RANGE_L, RobotConstants.SENSOR_SHORT_RANGE_H, this.posRow + 1, this.posCol, this.robotDir, "SRFC");
        SRFrontRight = new Sensor(RobotConstants.SENSOR_SHORT_RANGE_L, RobotConstants.SENSOR_SHORT_RANGE_H, this.posRow + 1, this.posCol + 1, this.robotDir, "SRFR");
        SRLeft = new Sensor(RobotConstants.SENSOR_SHORT_RANGE_L, RobotConstants.SENSOR_SHORT_RANGE_H, this.posRow + 1, this.posCol - 1, findNewDirection(MOVEMENT.LEFT), "SRL");
        SRRight = new Sensor(RobotConstants.SENSOR_SHORT_RANGE_L, RobotConstants.SENSOR_SHORT_RANGE_H, this.posRow + 1, this.posCol + 1, findNewDirection(MOVEMENT.RIGHT), "SRR");
        LRLeft = new Sensor(RobotConstants.SENSOR_LONG_RANGE_L, RobotConstants.SENSOR_LONG_RANGE_H, this.posRow, this.posCol - 1, findNewDirection(MOVEMENT.LEFT), "LRL");
    }

    public void setRobotPos(int row, int col) {
        posRow = row;
        posCol = col;
    }

    public int getRobotPosRow() {
        return posRow;
    }

    public int getRobotPosCol() {
        return posCol;
    }

    public void setRobotDir(DIRECTION dir) {
        robotDir = dir;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public DIRECTION getRobotCurDir() {
        return robotDir;
    }

    public boolean getRealBot() {
        return realBot;
    }

    private void updateTouchedGoal() {
        if (this.getRobotPosRow() == MapConstants.GOAL_ROW && this.getRobotPosCol() == MapConstants.GOAL_COL)
            this.touchedGoal = true;
    }

    public boolean getTouchedGoal() {
        return this.touchedGoal;
    }

    /**
     * Takes in a MOVEMENT and moves the robot accordingly by changing its position and direction. Sends the movement
     * if this.realBot is set.
     */
    public void move(MOVEMENT m, boolean sendMoveToAndroid) {
        if (!realBot) {
            // Emulate real movement by pausing execution.
            try {
                TimeUnit.MILLISECONDS.sleep(speed);
            } catch (InterruptedException e) {
                System.out.println("Something went wrong in Robot.move()!");
            }
        }

        switch (m) {
            case FORWARD:
                switch (robotDir) {
                    case NORTH:
                        posRow++;
                        break;
                    case EAST:
                        posCol++;
                        break;
                    case SOUTH:
                        posRow--;
                        break;
                    case WEST:
                        posCol--;
                        break;
                }
                break;
            case BACKWARD:
                switch (robotDir) {
                    case NORTH:
                        posRow--;
                        break;
                    case EAST:
                        posCol--;
                        break;
                    case SOUTH:
                        posRow++;
                        break;
                    case WEST:
                        posCol++;
                        break;
                }
                break;
            case RIGHT:
            case LEFT:
                robotDir = findNewDirection(m);
                break;
            case CALIBRATE:
                break;
            default:
                System.out.println("Error in Robot.move()!");
                break;
        }

        if (realBot) sendMovement(m, false);
        else System.out.println("Move: " + MOVEMENT.print(m));

        updateTouchedGoal();
    }

    /**
     * Overloaded method that calls this.move(MOVEMENT m, boolean sendMoveToAndroid = true).
     */
    public void move(MOVEMENT m) {
        this.move(m, true);
    }

    /**
     * Sends a number instead of 'F' for multiple continuous forward movements.
     */
    public void moveForwardMultiple(int count) {
        if (count == 1) {
            move(MOVEMENT.FORWARD);
        } else {
            CommMgr comm = CommMgr.getCommMgr();
            if (count == 10) {
                comm.sendMsg("0", CommMgr.INSTRUCTIONS);
            } else if (count < 10) {
                comm.sendMsg(Integer.toString(count), CommMgr.INSTRUCTIONS);
            }

            switch (robotDir) {
                case NORTH:
                    posRow += count;
                    break;
                case EAST:
                    posCol += count;
                    break;
                case SOUTH:
                    posRow += count;
                    break;
                case WEST:
                    posCol += count;
                    break;
            }

            comm.sendMsg(this.getRobotPosRow() + "," + this.getRobotPosCol() + "," + DIRECTION.print(this.getRobotCurDir()), CommMgr.BOT_POS);
        }
    }

    /**
     * Uses the CommMgr to send the next movement to the robot.
     */
    private void sendMovement(MOVEMENT m, boolean sendMoveToAndroid) {
        CommMgr comm = CommMgr.getCommMgr();
        comm.sendMsg(MOVEMENT.print(m) + "", CommMgr.INSTRUCTIONS);
        System.out.println("Bot Current Position: "+this.getRobotPosRow() + "," + this.getRobotPosCol());
        if (m != MOVEMENT.CALIBRATE && sendMoveToAndroid) {
            comm.sendMsg(this.getRobotPosRow() + "," + this.getRobotPosCol() + "," + DIRECTION.print(this.getRobotCurDir()), CommMgr.BOT_POS);
        }
    }

    /**
     * Sets the sensors' position and direction values according to the robot's current position and direction.
     */
    public void setSensors() {
        switch (robotDir) {
            case NORTH:
                SRFrontLeft.setSensor(this.posRow + 1, this.posCol - 1, this.robotDir);
                SRFrontCenter.setSensor(this.posRow + 1, this.posCol, this.robotDir);
                SRFrontRight.setSensor(this.posRow + 1, this.posCol + 1, this.robotDir);
                SRLeft.setSensor(this.posRow + 1, this.posCol - 1, findNewDirection(MOVEMENT.LEFT));
                LRLeft.setSensor(this.posRow, this.posCol - 1, findNewDirection(MOVEMENT.LEFT));
                SRRight.setSensor(this.posRow + 1, this.posCol + 1, findNewDirection(MOVEMENT.RIGHT));
                break;
            case EAST:
                SRFrontLeft.setSensor(this.posRow + 1, this.posCol + 1, this.robotDir);
                SRFrontCenter.setSensor(this.posRow, this.posCol + 1, this.robotDir);
                SRFrontRight.setSensor(this.posRow - 1, this.posCol + 1, this.robotDir);
                SRLeft.setSensor(this.posRow + 1, this.posCol + 1, findNewDirection(MOVEMENT.LEFT));
                LRLeft.setSensor(this.posRow + 1, this.posCol, findNewDirection(MOVEMENT.LEFT));
                SRRight.setSensor(this.posRow - 1, this.posCol + 1, findNewDirection(MOVEMENT.RIGHT));
                break;
            case SOUTH:
                SRFrontLeft.setSensor(this.posRow - 1, this.posCol + 1, this.robotDir);
                SRFrontCenter.setSensor(this.posRow - 1, this.posCol, this.robotDir);
                SRFrontRight.setSensor(this.posRow - 1, this.posCol - 1, this.robotDir);
                SRLeft.setSensor(this.posRow - 1, this.posCol + 1, findNewDirection(MOVEMENT.LEFT));
                LRLeft.setSensor(this.posRow, this.posCol + 1, findNewDirection(MOVEMENT.LEFT));
                SRRight.setSensor(this.posRow - 1, this.posCol - 1, findNewDirection(MOVEMENT.RIGHT));
                break;
            case WEST:
                SRFrontLeft.setSensor(this.posRow - 1, this.posCol - 1, this.robotDir);
                SRFrontCenter.setSensor(this.posRow, this.posCol - 1, this.robotDir);
                SRFrontRight.setSensor(this.posRow + 1, this.posCol - 1, this.robotDir);
                SRLeft.setSensor(this.posRow - 1, this.posCol - 1, findNewDirection(MOVEMENT.LEFT));
                LRLeft.setSensor(this.posRow - 1, this.posCol, findNewDirection(MOVEMENT.LEFT));
                SRRight.setSensor(this.posRow + 1, this.posCol - 1, findNewDirection(MOVEMENT.RIGHT));
                break;
        }

    }

    /**
     * Uses the current direction of the robot and the given movement to find the new direction of the robot.
     */
    private DIRECTION findNewDirection(MOVEMENT m) {
        if (m == MOVEMENT.RIGHT) {
            return DIRECTION.getNext(robotDir);
        } else {
            return DIRECTION.getPrevious(robotDir);
        }
    }

    /**
     * Calls the .sense() method of all the attached sensors and stores the received values in an integer array.
     *
     * @return [SRFrontLeft, SRFrontCenter, SRFrontRight, SRLeft, SRRight, LRLeft]
     */
    public int[] sense(Map explorationMap, Map realMap) {
        int[] result = new int[6];

        if (!realBot) {
            result[0] = SRFrontLeft.sense(explorationMap, realMap);
            result[1] = SRFrontCenter.sense(explorationMap, realMap);
            result[2] = SRFrontRight.sense(explorationMap, realMap);
            result[3] = SRLeft.sense(explorationMap, realMap);
            result[4] = SRRight.sense(explorationMap, realMap);
            result[5] = LRLeft.sense(explorationMap, realMap);
        } else {
            CommMgr comm = CommMgr.getCommMgr();
            String msg = comm.recvMsg();
            String[] msgArr = msg.split("\\|");
            result[0] = Integer.parseInt(msgArr[0].substring(msgArr[0].length()-1));
            result[1] = Integer.parseInt(msgArr[1]);
            result[2] = Integer.parseInt(msgArr[2]);
            result[3] = Integer.parseInt(msgArr[3]);
            result[4] = Integer.parseInt(msgArr[4]);
            result[5] = Integer.parseInt(msgArr[5]);
            //System.out.println("msgArr[6] is ...."+msgArr[6]);
            SRFrontLeft.senseReal(explorationMap, result[1]);
            SRFrontCenter.senseReal(explorationMap, result[2]);
            SRFrontRight.senseReal(explorationMap, result[3]);
            SRLeft.senseReal(explorationMap, result[4]);
            SRRight.senseReal(explorationMap, result[5]);
            LRLeft.senseReal(explorationMap, result[0]);

            String[] mapStrings = MapDescriptor.generateMapDescriptor(explorationMap);
            comm.sendMsg("md"+mapStrings[0] + " " + mapStrings[1] + " " + this.getRobotPosRow() + " " + this.getRobotPosCol() + " " + DIRECTION.print(this.getRobotCurDir()), CommMgr.MAP_STRINGS);
        }

        return result;
    }
}
