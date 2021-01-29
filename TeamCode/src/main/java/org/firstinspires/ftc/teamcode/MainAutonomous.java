package org.firstinspires.ftc.teamcode;/*
package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

*/

import com.acmerobotics.dashboard.FtcDashboard;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * MainAutonomous.java
 *
 *
 * A Linear OpMode class to be an autonomous method for both Blue & Red alliance sides where we pick
 * which side of the alliance bridge we start off at with gamepad1 as well as selecting alliance
 * color and whether we park under the alliance bridge closer or further from the game field wall.
 * Also will detect the position and deliver the skystone using machine vision and move the
 * foundation.
 *
 * Mec_Odo_AutonomousLevel6_Statey is written to use machine vision and SkyStone delivery to our
 * autonomous route with the help intake jaws that suck in a stone at any orientation using a
 * "touch it-own it" approach.  A servo and two motors make up TC-73/Bucky's arm and stack stones as
 * well as our team marker.

 * This autonomous is used for our State Championship(February 7-8, 2020).
 *
 *
 * @author FTC Team #10273, The Cat in the Hat Comes Back.
 */

@Autonomous(name="MainAutonomous", group="CatAuto")

public class MainAutonomous extends LinearOpMode
{

/* Declare OpMode members. */

    CatHW_Async robot  = new CatHW_Async();    // All the hardware classes init here.
    private ElapsedTime delayTimer = new ElapsedTime();
    private double timeDelay;
    private boolean isRedAlliance = true;
    private boolean isPowerShot = true;



    @Override
    public void runOpMode() throws InterruptedException {

        /*
        Initialize the setDrivePowers system variables.  The init() methods of our hardware class
        does all the work:
         */
        FtcDashboard dashboard = FtcDashboard.getInstance();
        Telemetry dashboardTelemetry = dashboard.getTelemetry();

        robot.init(hardwareMap, this, true);



/*
        Init Delay Option Select:
         */

        // After init is pushed but before Start we can change the delay using dpad up/down //
        delayTimer.reset();
        // Runs a loop to change certain settings while we wait to start
        while (!opModeIsActive()) {
            if (this.isStopRequested()) {
                // Leave the loop if STOP is pressed
                return;
            }
            if (gamepad1.dpad_up && (delayTimer.seconds() > 0.8)) {
                // Increases the amount of time we wait
                timeDelay += 1;
                delayTimer.reset();
            }
            if (gamepad1.dpad_down && (delayTimer.seconds() > 0.8)) {
                // Decreases the amount of time we wait
                if (timeDelay > 0) {
                    // No such thing as negative time
                    timeDelay -= 1;
                }
                delayTimer.reset();
            }
            if (((gamepad1.x) && delayTimer.seconds() > 0.8)) {
                // Changes Alliance Sides
                if (isRedAlliance) {
                    isRedAlliance = false;
                    robot.isRedAlliance = false;
                } else {
                    isRedAlliance = true;
                    robot.isRedAlliance = true;
                }
                delayTimer.reset();
            }
            if (gamepad1.b && (delayTimer.seconds() > 0.8)) {
                isPowerShot = !isPowerShot;
                delayTimer.reset();
            }

            //Allow the intake to run in autonomous
            robot.jaws.setJawPower(gamepad1.right_trigger - (gamepad1.left_trigger * 0.3));
            if (robot.jaws.getJawPower() > 0.05) {
                robot.jaws.setTransferPower(0.6);
            } else {
                robot.jaws.setTransferPower(0);
            }


            /**
             * LED code:
             */

            if (robot.eyes.getNumRings() == CatHW_Vision.UltimateGoalPipeline.numRings.NONE) {
                robot.lights.setDefaultColor(RevBlinkinLedDriver.BlinkinPattern.RAINBOW_LAVA_PALETTE);
            }
            if (robot.eyes.getNumRings() == CatHW_Vision.UltimateGoalPipeline.numRings.ONE) {
                robot.lights.setDefaultColor(RevBlinkinLedDriver.BlinkinPattern.RAINBOW_OCEAN_PALETTE);
            }
            if (robot.eyes.getNumRings() == CatHW_Vision.UltimateGoalPipeline.numRings.FOUR) {
                robot.lights.setDefaultColor(RevBlinkinLedDriver.BlinkinPattern.RAINBOW_PARTY_PALETTE);
            }


            /**
             * Telemetry while waiting for PLAY:
             */

            telemetry.addData("Delay Timer: ", timeDelay);

            if (isRedAlliance) {
                telemetry.addData("Alliance: ", "Red");
            } else {
                telemetry.addData("Alliance: ", "Blue");
            }
            if (isPowerShot) {
                telemetry.addData("Goal: ", "Power Shot");
            } else {
                telemetry.addData("Goal: ", "High Goal");
            }
            telemetry.addData("Num of Rings", "%s", robot.eyes.getNumRings().toString());
            dashboardTelemetry.addData("Num of Rings", "%s", robot.eyes.getNumRings().toString());
            dashboardTelemetry.addData("Analysis", "%d", robot.eyes.pipeline.getAnalysis());

            dashboardTelemetry.update();

            telemetry.update();


            /**
             * We don't need a "waitForStart()" since we've been running our own
             * loop all this time so that we can make some changes.
             */

        }
        CatHW_Vision.UltimateGoalPipeline.numRings numRings = robot.eyes.getNumRings();

        /**
         * Runs after hit start:
         * DO STUFF FOR the OPMODE!!!
         */


        /**
         * Init the IMU after play so that it is not offset after
         * remaining idle for a minute or two...
         */

        robot.driveClassic.IMU_Init();

        // Time Delay:
        robot.robotWait(timeDelay);
        //powers on launcher
        robot.launcher.powerOn();
        if (isPowerShot) {
            robot.launcher.presetPowerShot();
            robot.launcher.aimR();

        } else {
            robot.launcher.aimHigh();
        }
        robot.robotWait(1);
        //drives to position to shoot rings
        robot.driveOdo.quickDrive(4, 48, 0.5, 0, 5.0);

        //shoots rings
        if (isPowerShot) {
            robot.launcher.shootPowerShots();
        } else {
            robot.launcher.shootRings();
        }
        robot.launcher.aimHigh();
        robot.driveOdo.quickDrive(1,1,0.4,0,5);
        robot.robotWait(2);

        numRings = CatHW_Vision.UltimateGoalPipeline.numRings.ONE;
        switch (numRings){
            case NONE:
                driveNone();
                break;
            case ONE:
               // driveOne();
                break;
            case FOUR:
                driveFour();
                break;
        }


        robot.driveOdo.updatesThread.stop();
    }
    public void driveNone(){
        //drives to position to drop first wobble goal and places first wobble goal
        robot.driveOdo.quickDrive(-4,84,0.7,-90,3.0);
        robot.tail.setArmDown();
        robot.robotWait(1);
        robot.tail.openGrabber();
        robot.robotWait(.5);

        //drives to position to grab second wobble goal
        robot.driveOdo.quickDrive(-30,37,0.7,0,5.0);
        robot.driveOdo.quickDrive(-29,27,0.7,0,5.0);

        //Grabs second wobble goal
        robot.tail.closeGrabber();
        robot.robotWait(.5);
        robot.tail.setArmUp();
        robot.robotWait(1);

        //drives to position to drop second wobble goal
        robot.driveOdo.quickDrive(-4,76,0.5,-90,4.0);

        //places second wobble goal
        robot.tail.setArmDown();
        robot.robotWait(1);
        robot.tail.openGrabber();

        //backs up to the white line
        robot.driveOdo.quickDrive(-12,76,0.5,-90,3.0);
    }
    public void driveOne(){
        //drives to position to drop first wobble goal and places first wobble goal
        robot.driveOdo.quickDrive(-10,100,1.0,90,5.0);
        robot.tail.setArmDown();
        robot.robotWait(1);
        robot.tail.openGrabber();
        robot.robotWait(.5);
        robot.driveOdo.quickDrive(-5,100,1.0,90,3.0);

        //drives to position to grab second wobble goal
        robot.driveOdo.quickDrive(-40,60,0.6,0,5.0);
        robot.driveOdo.quickDrive(-36,26,0.6,-15,5.0);

        //grabs second wobble goal
        robot.tail.closeGrabber();
        robot.robotWait(.5);
        robot.tail.setArmUp();
        robot.robotWait(1);

        //goes to position to grab the ring
        robot.driveOdo.quickDrive(-22,30,0.6,0,5.0);
        robot.launcher.powerOn();
        robot.jaws.setJawPower(1.0);
        robot.jaws.setTransferPower(0.6);
        robot.driveOdo.quickDrive(-22,34,0.7,0,5.0);
        robot.robotWait(1.0);

        //goes to position to shoot ring and shoots ring
        robot.driveOdo.quickDrive(-22,34,0.7,20,5.0);
        delayTimer.reset();
        robot.launcher.openLauncher();
        robot.jaws.setTransferPower(.7);

        robot.robotWait(2.0);
        robot.jaws.setTransferPower(0);
        robot.launcher.closeLauncher();

        //drives to position to place second wobble goal and places second wobble goal
        robot.driveOdo.quickDrive(-30,81,0.6,180,5.0);
        robot.tail.setArmDown();
        robot.robotWait(1);
        robot.tail.openGrabber();

        //drives back to white line
        robot.driveOdo.quickDrive(-30,75,0.6,180,3.0);
    }
    public void driveFour(){
        //drives to position to drop first wobble goal and places first wobble goal
        robot.launcher.powerOn();
        robot.driveOdo.quickDrive(5,116,9.0,-180,5.0);
        robot.tail.setArmDown();
        robot.robotWait(1);
        robot.tail.openGrabber();
        robot.robotWait(.5);
        robot.driveOdo.quickDrive(5,105,1.0,-180,5.0);

        //drives to pick up second wobble goal
        robot.driveOdo.quickDrive(-40,60,0.9,0,5.0);
        robot.driveOdo.quickDrive(-40,24,0.6,-20,7.0);
        robot.tail.closeGrabber();
        robot.robotWait(.5);
        robot.tail.setArmUp();
        robot.robotWait(1.0);

        //drives to pick up rings
        robot.driveOdo.quickDrive(-17,32,0.6,0,5.0);
        robot.jaws.setJawPower(1.0);
        robot.jaws.setTransferPower(0.6);
        robot.driveOdo.quickDrive(-17,34,0.6,0,5.0);
        robot.driveOdo.quickDrive(-17,34,0.6,20,5.0);

        //shoots rings
        delayTimer.reset();
        robot.launcher.openLauncher();
        robot.jaws.setTransferPower(.7);
        robot.driveOdo.quickDrive(-17,40,0.3,20,5.0);

        robot.robotWait(3.0);
        robot.jaws.setTransferPower(0);
        robot.launcher.closeLauncher();

        //drives to drop off second wobble goal and backs up to the line
        robot.driveOdo.quickDrive(5,107,1.0,-180,5.0);
        robot.tail.setArmDown();
        robot.robotWait(1);
        robot.tail.openGrabber();
        robot.robotWait(.5);
        robot.driveOdo.quickDrive(5,75,1.0,-180,3.0);









    }

}
