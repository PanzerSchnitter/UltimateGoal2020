package org.firstinspires.ftc.teamcode;

import android.util.Log;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.ReadWriteFile;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.openftc.revextensions2.ExpansionHubEx;
import org.openftc.revextensions2.RevBulkData;

import java.io.File;

//import org.openftc.revextensions2.ExpansionHubEx;
//import org.openftc.revextensions2.RevBulkData;

/**
 * CatOdoPositionUpdate.java
 *
 *
 * Class to keep track of robot's position using odometry modules.  Since our robot moves fast and
 * the data reading ends up not being consistent with the brief time lapse.  Thus, we added bulk
 * reads along with several different position equation changes and other work.
 *
 *
 * @author Original created by Sarthak (of Wizards.exe) on 6/1/2019.
 * @author Modified by Team #10273, The Cat in the Hat Comes Back.
 */
public class CatOdoPositionUpdate
{

    //----------------------------------------------------------------------------------------------
    // Attributes:
    //----------------------------------------------------------------------------------------------

    // Odometry wheels:
    private DcMotorEx verticalEncoderLeft = null;
    private DcMotorEx verticalEncoderRight = null;
    private DcMotorEx horizontalEncoder = null;
    // Expansion hubs:
    public HardwareMap hwMap = null;

    //private ExpansionHubEx expansionHub;

    // Position variables used for storage and calculations:
    private double verticalRightEncoderWheelPosition = 0;
    private double verticalLeftEncoderWheelPosition = 0;
    private double normalEncoderWheelPosition = 0;
    private double changeInRobotOrientation = 0;
    private double robotGlobalXCoordinatePosition = 0;
    private double robotGlobalYCoordinatePosition = 0;
    private double robotOrientationRadians = 0;
    private double previousVerticalRightEncoderWheelPosition = 0;
    private double previousVerticalLeftEncoderWheelPosition = 0;
    private double prevNormalEncoderWheelPosition = 0;

    // Algorithm constants:
    public double robotEncoderWheelDistance = 0;
    private double horizontalEncoderTickPerDegreeOffset = 0;

    // Files to access the algorithm constants:
    private final File wheelBaseSeparationFile = AppUtil.getInstance().getSettingsFile(
            "wheelBaseSeparation.txt");
    private final File horizontalTickOffsetFile = AppUtil.getInstance().getSettingsFile(
            "horizontalTickOffset.txt");

    private int leftEncoderValue = 0;
    private int rightEncoderValue = 0;
    private int horizontalEncoderValue = 0;

    private ElapsedTime time = new ElapsedTime();

    public double count_per_in = 0;

    public boolean isUpdated = false;


    /**
     * Constructor for GlobalCoordinatePosition Thread
     *
     * @param verticalEncoderLeft left odometry encoder, facing the vertical direction
     * @param verticalEncoderRight right odometry encoder, facing the vertical direction
     * @param horizontalEncoder horizontal odometry encoder, perpendicular to the other two odometry
     *                          encoder wheels
     */
    public CatOdoPositionUpdate(HardwareMap hwMap, DcMotor verticalEncoderLeft,
                                DcMotor verticalEncoderRight, DcMotor horizontalEncoder,
                                double COUNTS_PER_INCH) {

        count_per_in = COUNTS_PER_INCH;
        this.hwMap = hwMap;
        this.verticalEncoderLeft = (DcMotorEx) verticalEncoderLeft;
        this.verticalEncoderRight = (DcMotorEx) verticalEncoderRight;
        this.horizontalEncoder = (DcMotorEx) horizontalEncoder;

        robotEncoderWheelDistance = Double.parseDouble(ReadWriteFile.readFile(wheelBaseSeparationFile).trim()) * COUNTS_PER_INCH;
        this.horizontalEncoderTickPerDegreeOffset = Double.parseDouble(ReadWriteFile.readFile(horizontalTickOffsetFile).trim());
        time.reset();
    }

    /**
     * Second constructor.
     *
     * @param hwMap
     * @param verticalEncoderLeft
     * @param verticalEncoderRight
     * @param horizontalEncoder
     * @param COUNTS_PER_INCH
     * @param startingX
     * @param startingY
     * @param startingOrientation
     */
    public CatOdoPositionUpdate(HardwareMap hwMap, DcMotor verticalEncoderLeft,
                                DcMotor verticalEncoderRight, DcMotor horizontalEncoder,
                                double COUNTS_PER_INCH, double startingX, double startingY,
                                double startingOrientation) {
        count_per_in = COUNTS_PER_INCH;
        this.hwMap = hwMap;
        this.verticalEncoderLeft = (DcMotorEx) verticalEncoderLeft;
        this.verticalEncoderRight = (DcMotorEx) verticalEncoderRight;
        this.horizontalEncoder = (DcMotorEx) horizontalEncoder;

        this.robotGlobalXCoordinatePosition = startingX;
        this.robotGlobalYCoordinatePosition = startingY;
        this.robotOrientationRadians = Math.toRadians(startingOrientation);

        robotEncoderWheelDistance = Double.parseDouble(ReadWriteFile.readFile(wheelBaseSeparationFile).trim()) * COUNTS_PER_INCH;
        this.horizontalEncoderTickPerDegreeOffset = Double.parseDouble(ReadWriteFile.readFile(horizontalTickOffsetFile).trim());
    }



    //----------------------------------------------------------------------------------------------
    // Class Methods:
    //----------------------------------------------------------------------------------------------

    /**
     * Updates the global (x, y, theta) coordinate position of the robot using the odometry encoders
     */
    public void globalCoordinatePositionUpdate() {
        // Do a bulk read of encoders:
        ExpansionHubEx controlHub   = hwMap.get(ExpansionHubEx.class, "Control Hub 1");

        RevBulkData bulkDataControl = controlHub.getBulkInputData();
        //TODO check the bulk data later for where odo ports are
        leftEncoderValue = bulkDataControl.getMotorCurrentPosition(verticalEncoderLeft);
        rightEncoderValue = bulkDataControl.getMotorCurrentPosition(verticalEncoderRight);
        horizontalEncoderValue = bulkDataControl.getMotorCurrentPosition(horizontalEncoder);

        // Get current positions:
        verticalLeftEncoderWheelPosition = leftEncoderValue;
        verticalRightEncoderWheelPosition = rightEncoderValue;

        // Find the difference between old and new odo values:
        double leftChange = verticalLeftEncoderWheelPosition - previousVerticalLeftEncoderWheelPosition;
        double rightChange = verticalRightEncoderWheelPosition - previousVerticalRightEncoderWheelPosition;

        // Calculate angle:
        changeInRobotOrientation = (leftChange - rightChange) / (robotEncoderWheelDistance);
        robotOrientationRadians = ((robotOrientationRadians + changeInRobotOrientation));
        double robotOrientationRadiansHalf = robotOrientationRadians -
                (changeInRobotOrientation / 2);

        // Get the components of the motion:
        normalEncoderWheelPosition = horizontalEncoderValue;
        double rawHorizontalChange = normalEncoderWheelPosition - prevNormalEncoderWheelPosition;
        double horizontalChange = rawHorizontalChange - (changeInRobotOrientation *
                horizontalEncoderTickPerDegreeOffset);

        double p = ((rightChange + leftChange) / 2);
        double n = horizontalChange;

        // Calculate and update the position values:
        double robotXChangeCounts = (p*Math.sin(robotOrientationRadiansHalf) + n*Math.cos(robotOrientationRadiansHalf));
        double robotYChangeCounts = (p*Math.cos(robotOrientationRadiansHalf) - n*Math.sin(robotOrientationRadiansHalf));
        robotGlobalXCoordinatePosition = robotGlobalXCoordinatePosition + robotXChangeCounts;
        robotGlobalYCoordinatePosition = robotGlobalYCoordinatePosition + robotYChangeCounts;
        // Assign the "new" odo values to the old values for next check:
        previousVerticalLeftEncoderWheelPosition = verticalLeftEncoderWheelPosition;
        previousVerticalRightEncoderWheelPosition = verticalRightEncoderWheelPosition;
        prevNormalEncoderWheelPosition = normalEncoderWheelPosition;

        // Calculate velocity:
        double velocityTimer = time.seconds();
        time.reset();

        double rotVelocity = (changeInRobotOrientation * (180/Math.PI)) / velocityTimer;
        double velocity = (Math.sqrt(Math.pow(robotXChangeCounts,2) +
                Math.pow(robotYChangeCounts,2))/count_per_in)/velocityTimer;

        Log.d("catbot", String.format("OdoTicks L/R/B  :%7d  :%7d  :%7d   X: %5.2f  Y: %5.2f  theta: %5.2f Velocity: %5.2f RotVelocity: %5.2f",
                returnVerticalLeftEncoderPosition(),
                returnVerticalRightEncoderPosition(),
                returnHorizontalEncoderPosition(),
                returnXInches(),
                returnYInches(),
                returnOrientation(),
                velocity,
                rotVelocity));


        isUpdated = true;
    }

    public double returnXInches() { return robotGlobalXCoordinatePosition/count_per_in; }

    public double returnYInches() { return robotGlobalYCoordinatePosition/count_per_in; }

    public CatType_Point returnRobotPointInches() {
        return new CatType_Point(robotGlobalXCoordinatePosition/count_per_in,
                robotGlobalYCoordinatePosition/count_per_in);
    }

    public void resetPos() {
        verticalRightEncoderWheelPosition = 0;
        verticalLeftEncoderWheelPosition = 0;
        normalEncoderWheelPosition = 0;
        changeInRobotOrientation = 0;
        robotGlobalXCoordinatePosition = 0;
        robotGlobalYCoordinatePosition = 0;
        robotOrientationRadians = 0;
        previousVerticalRightEncoderWheelPosition = 0;
        previousVerticalLeftEncoderWheelPosition = 0;
        prevNormalEncoderWheelPosition = 0;
        leftEncoderValue = 0;
        rightEncoderValue = 0;
        horizontalEncoderValue = 0;

    }

    /**
     * Returns the robot's global x coordinate
     * @return global x coordinate
     */
    public double returnXCoordinate() { return robotGlobalXCoordinatePosition; }

    /**
     * Returns the robot's global y coordinate
     * @return global y coordinate
     */
    public double returnYCoordinate() { return robotGlobalYCoordinatePosition; }

    /**
     * Returns the robot's global orientation
     * @return global orientation, in degrees
     */
    public double returnOrientation() { return Math.toDegrees(robotOrientationRadians); }

    public int returnVerticalLeftEncoderPosition() {
        return leftEncoderValue;
    }

    public int returnVerticalRightEncoderPosition() {
        return rightEncoderValue;
    }

    public int returnHorizontalEncoderPosition() {
        return horizontalEncoderValue;
    }


    public void setAngleOffset(double offset){
        robotOrientationRadians += Math.toRadians(offset);
    }

    public void setRobotGlobalCoordinate(double x, double y, double theta) {
        robotGlobalXCoordinatePosition = x;
        robotGlobalYCoordinatePosition = y;
        robotOrientationRadians = Math.toRadians(theta);
    }

}