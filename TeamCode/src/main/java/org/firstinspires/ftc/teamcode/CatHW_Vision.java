package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XYZ;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.YZX;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesReference.EXTRINSIC;

/**
 * CatHW_Vision.java
 *
 *
 * A "hardware" class intended to contain common code for accessing camera and other vision related
 * situations.  While previous versions were made to mostly to test various forms of machine vision,
 * this version uses the Tensor Flow system from the FTC SDK to detect the SkyStones during init in
 * our autonomous routines. We've also tested Vuforia.  TODO:  Check if this is correct...
 *
 *
 * This is NOT an OpMode.  This class is used to define all the other hardware classes.
 * This hardware class assumes the following device names have been configured on the robot.
 *
 *
 * @author FTC Team #10273, The Cat in the Hat Comes Back
 */
public class CatHW_Vision extends CatHW_Subsystem
{
    public static class UltimateGoalPipeline extends OpenCvPipeline
    {
        public static int regionWidth = 80;
        public static int regionHeight = 50;
        /*
         * Some color constants
         */
        static final Scalar BLUE = new Scalar(0, 0, 255);
        static final Scalar GREEN = new Scalar(0, 255, 0);

        /*
         * The core values which define the location and size of the sample regions
         */
        static final Point REGION1_TOPLEFT_ANCHOR_POINT = new Point(160,10);

        static int REGION_WIDTH = regionWidth;
        static int REGION_HEIGHT = regionHeight;

        final int FOUR_RING_THRESHOLD = 80;
        final int ONE_RING_THRESHOLD = 40;

        Point region1_pointA = new Point(
                REGION1_TOPLEFT_ANCHOR_POINT.x,
                REGION1_TOPLEFT_ANCHOR_POINT.y);
        Point region1_pointB = new Point(
                REGION1_TOPLEFT_ANCHOR_POINT.x + regionWidth,
                REGION1_TOPLEFT_ANCHOR_POINT.y + regionHeight);

        /*
         * Working variables
         */
        Mat region1_Cb;
        Mat YCrCb = new Mat();
        Mat Cb = new Mat();
        int avg1;
        Mat hsv = new Mat();

        // Volatile since accessed by OpMode thread w/o synchronization
        private volatile numRings position = numRings.FOUR;


        /* Enums */
        enum numRings {
            NONE,
            ONE,
            FOUR
        }
        private Deque<numRings> ringValues;
        public numRings avgValue = numRings.ONE;

        @Override
        public void init(Mat firstFrame)
        {
            inputToCb(firstFrame);

            region1_Cb = Cb.submat(new Rect(region1_pointA, region1_pointB));

        }

        @Override
        public Mat processFrame(Mat input)
        {
            region1_pointB.x = REGION1_TOPLEFT_ANCHOR_POINT.x + regionWidth;
            region1_pointB.y = REGION1_TOPLEFT_ANCHOR_POINT.y + regionHeight;

            inputToCb(input);

            avg1 = (int) Core.mean(region1_Cb).val[0];

            Imgproc.rectangle(
                    input, // Buffer to draw on
                    region1_pointA, // First point which defines the rectangle
                    region1_pointB, // Second point which defines the rectangle
                    BLUE, // The color the rectangle is drawn in
                    2); // Thickness of the rectangle lines

            position = numRings.FOUR; // Record our analysis
            if(avg1 > FOUR_RING_THRESHOLD){
                position = numRings.FOUR;
            }else if (avg1 > ONE_RING_THRESHOLD){
                position = numRings.ONE;
            }else {
                position = numRings.NONE;
            }



            if (ringValues.size() > 29) {
                // Make sure we keep the size at a reasonable level
                ringValues.removeFirst();
            }
            ringValues.add(position);
            if (Collections.frequency(ringValues, UltimateGoalPipeline.numRings.ONE) > Collections.frequency(ringValues, UltimateGoalPipeline.numRings.FOUR) &&
                    Collections.frequency(ringValues, UltimateGoalPipeline.numRings.ONE) > Collections.frequency(ringValues, UltimateGoalPipeline.numRings.NONE)) {
                // If the amount of INSIDE readings is the most in the past 30 readings, return INSIDE.
                avgValue=numRings.ONE;
            } else if (Collections.frequency(ringValues, UltimateGoalPipeline.numRings.FOUR) > Collections.frequency(ringValues, UltimateGoalPipeline.numRings.ONE) &&
                    Collections.frequency(ringValues, UltimateGoalPipeline.numRings.FOUR) > Collections.frequency(ringValues, UltimateGoalPipeline.numRings.NONE)) {
                // If the amount of CENTER readings is the most in the past 30 readings, return CENTER.
                avgValue=numRings.FOUR;
            } else {
                // Just return back OUTSIDE since it is the last possible value.
               avgValue=numRings.NONE;
            }

            return input;
        }
        void inputToCb(Mat input)
        {
            Imgproc.cvtColor(input, hsv, Imgproc.COLOR_RGB2HSV);
            Core.extractChannel(hsv, Cb, 1);
        }



        public int getAnalysis()
        {
            return avg1;
        }

    }


    UltimateGoalPipeline pipeline;
    OpenCvCamera webcam;


    private HardwareMap hwMap   = null;
    private VuforiaLocalizer vuforia = null;
    private VuforiaTrackables targetsUltimateGoal = null;
    public List<VuforiaTrackable> allTrackables = null;
    // Since ImageTarget trackables use mm to specifiy their dimensions, we must use mm for all the physical dimension.
    // We will define some constants and conversions here
    public static final float mmPerInch         = 25.4f;
    private static final float mmTargetHeight   = (6) * mmPerInch;          // the height of the center of the target image above the floor

    // Constants for perimeter targets
    private static final float halfField = 72 * mmPerInch;
    private static final float quadField  = 36 * mmPerInch;
    private static final float fullField  = 144 * mmPerInch;

    /* Constructor */
    public CatHW_Vision(CatHW_Async mainHardware){
        super(mainHardware);
    }

    /**
     * Initializes the "hardware" devices for anything having to do with machine vision.
     *
     * @param ahwMap which contains the hardware to look for.
     */
    public void initVision(HardwareMap ahwMap, boolean useVuoria) {

        hwMap = ahwMap;
        if (useVuoria) {
            initVuforia();
        } else {
            int cameraMonitorViewId = hwMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hwMap.appContext.getPackageName());
            webcam = OpenCvCameraFactory.getInstance().createWebcam(hwMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId);
            pipeline = new UltimateGoalPipeline();
            pipeline.ringValues = new ArrayDeque<>(30);

            webcam.setPipeline(pipeline);
            webcam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
                @Override
                public void onOpened() {
                    webcam.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);
                    FtcDashboard.getInstance().startCameraStream(webcam, 10);
                }
            });
        }
    }
    public void stop(){
        webcam.closeCameraDevice();
        if(targetsUltimateGoal != null){targetsUltimateGoal.deactivate();}

    }

    private void initVuforia () {
        WebcamName webcamName = hwMap.get(WebcamName.class, "Webcam 1");

        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         * We can pass Vuforia the handle to a camera preview resource (on the RC phone);
         * If no camera monitor is desired, use the parameter-less constructor instead (commented out below).
         */
        int cameraMonitorViewId = hwMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hwMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);

        // VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = "AWbfTmn/////AAABmY0xuIe3C0RHvL3XuzRxyEmOT2OekXBSbqN2jot1si3OGBObwWadfitJR/D6Vk8VEBiW0HG2Q8UAEd0//OliF9aWCRmyDJ1mMqKCJZxpZemfT5ELFuWnJIZWUkKyjQfDNe2RIaAh0ermSxF4Bq77IDFirgggdYJoRIyi2Ys7Gl9lD/tSonV8OnldIN/Ove4/MtEBJTKHqjUEjC5U2khV+26AqkeqbxhFTNiIMl0LcmSSfugGhmWFGFtuPtp/+flPBRGoBO+tSl9P2sV4mSUBE/WrpHqB0Jd/tAmeNvbtgQXtZEGYc/9NszwRLVNl9k13vrBcgsiNxs2UY5xAvA4Wb6LN7Yu+tChwc+qBiVKAQe09\n";;

        /**
         * We also indicate which camera on the RC we wish to use.
         */
        parameters.cameraName = webcamName;

        // Make sure extended tracking is disabled for this example.
        parameters.useExtendedTracking = false;

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Load the data sets for the trackable objects. These particular data
        // sets are stored in the 'assets' part of our application.
        targetsUltimateGoal = this.vuforia.loadTrackablesFromAsset("UltimateGoal");
        //VuforiaTrackable blueTowerGoalTarget = targetsUltimateGoal.get(0);
        //blueTowerGoalTarget.setName("Blue Tower Goal Target");
        VuforiaTrackable redTowerGoalTarget = targetsUltimateGoal.get(1);
        redTowerGoalTarget.setName("Red Tower Goal Target");
        VuforiaTrackable redAllianceTarget = targetsUltimateGoal.get(2);
        redAllianceTarget.setName("Red Alliance Target");
        //VuforiaTrackable blueAllianceTarget = targetsUltimateGoal.get(3);
        //blueAllianceTarget.setName("Blue Alliance Target");
        VuforiaTrackable frontWallTarget = targetsUltimateGoal.get(4);
        frontWallTarget.setName("Front Wall Target");

        // For convenience, gather together all the trackable objects in one easily-iterable collection */
        allTrackables = new ArrayList<VuforiaTrackable>();
        allTrackables.addAll(targetsUltimateGoal);

        /**
         * In order for localization to work, we need to tell the system where each target is on the field, and
         * where the phone resides on the robot.  These specifications are in the form of <em>transformation matrices.</em>
         * Transformation matrices are a central, important concept in the math here involved in localization.
         * See <a href="https://en.wikipedia.org/wiki/Transformation_matrix">Transformation Matrix</a>
         * for detailed information. Commonly, you'll encounter transformation matrices as instances
         * of the {@link OpenGLMatrix} class.
         *
         * If you are standing in the Red Alliance Station looking towards the center of the field,
         *     - The X axis runs from your left to the right. (positive from the center to the right)
         *     - The Y axis runs from the Red Alliance Station towards the other side of the field
         *       where the Blue Alliance Station is. (Positive is from the center, towards the BlueAlliance station)
         *     - The Z axis runs from the floor, upwards towards the ceiling.  (Positive is above the floor)
         *
         * Before being transformed, each target image is conceptually located at the origin of the field's
         *  coordinate system (the center of the field), facing up.
         */

        //Set the position of the perimeter targets with relation to origin (center of field)
        redAllianceTarget.setLocation(OpenGLMatrix
                .translation((16 * mmPerInch), (halfField - (8 * mmPerInch)), mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, -90)));

        //blueAllianceTarget.setLocation(OpenGLMatrix
        //        .translation(0, halfField, mmTargetHeight)
        //        .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 0)));
        frontWallTarget.setLocation(OpenGLMatrix
                .translation(-(56 * mmPerInch),  -(8 * mmPerInch), mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0 , 0)));

        // The tower goal targets are located a quarter field length from the ends of the back perimeter wall.
        //blueTowerGoalTarget.setLocation(OpenGLMatrix
        //        .translation(halfField, quadField, mmTargetHeight)
        //        .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0 , -90)));
        redTowerGoalTarget.setLocation(OpenGLMatrix
                .translation(-(20 * mmPerInch), (fullField - (8 * mmPerInch)), mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 180)));

        //
        // Create a transformation matrix describing where the phone is on the robot.
        //
        // NOTE !!!!  It's very important that you turn OFF your phone's Auto-Screen-Rotation option.
        // Lock it into Portrait for these numbers to work.
        //
        // Info:  The coordinate frame for the robot looks the same as the field.
        // The robot's "forward" direction is facing out along X axis, with the LEFT side facing out along the Y axis.
        // Z is UP on the robot.  This equates to a bearing angle of Zero degrees.
        //
        // The phone starts out lying flat, with the screen facing Up and with the physical top of the phone
        // pointing to the LEFT side of the Robot.
        // The two examples below assume that the camera is facing forward out the front of the robot.

        // We need to rotate the camera around it's long axis to bring the correct camera forward.
/*        if (CAMERA_CHOICE == BACK) {
            phoneYRotate = -90;
        } else {
            phoneYRotate = 90;
        }

        // Rotate the phone vertical about the X axis if it's in portrait mode
        if (PHONE_IS_PORTRAIT) {
            phoneXRotate = 90 ;
        }
*/
        // Next, translate the camera lens to where it is on the robot.
        // In this example, it is centered (left to right), but forward of the middle of the robot, and above ground level.
        final float CAMERA_FORWARD_DISPLACEMENT  = 5.5f * mmPerInch;   // eg: Camera is 4 Inches in front of robot-center
        final float CAMERA_VERTICAL_DISPLACEMENT = 6.25f * mmPerInch;   // eg: Camera is 8 Inches above ground
        final float CAMERA_LEFT_DISPLACEMENT     = -0.5f * mmPerInch;     // eg: Camera is ON the robot's center line

        OpenGLMatrix robotFromCamera = OpenGLMatrix
                .translation(CAMERA_FORWARD_DISPLACEMENT, CAMERA_LEFT_DISPLACEMENT, CAMERA_VERTICAL_DISPLACEMENT)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, YZX, DEGREES, -45, 0, 0));

        /**  Let all the trackable listeners know where the phone is.  */
        for (VuforiaTrackable trackable : allTrackables) {
            ((VuforiaTrackableDefaultListener) trackable.getListener()).setPhoneInformation(robotFromCamera, parameters.cameraDirection);
        }

        // Note: To use the remote camera preview:
        // AFTER you hit Init on the Driver Station, use the "options menu" to select "Camera Stream"
        // Tap the preview window to receive a fresh image.

        targetsUltimateGoal.activate();
    }
    //----------------------------------------------------------------------------------------------
    // Open CV Metthods:
    //----------------------------------------------------------------------------------------------


    /**
     * A new way to take the all the values during the init and choosing the value in the deque that
     * has the most occurrences.
     *
     * @return which SkyStone position is the most likely.
     */
    public UltimateGoalPipeline.numRings getNumRings() {
        return pipeline.avgValue;
    }
}