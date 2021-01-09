package org.firstinspires.ftc.teamcode;

import android.util.Log;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * CatHW_Launcher.java
 *
 *
 * A "hardware" class containing common code accessing hardware specific to the movement and
 * rotation of the Launcher.
 *
 *
 * This is NOT an OpMode.  This class is used to define all the other hardware classes.
 * This hardware class assumes the following device names have been configured on the robot.
 *
 *
 * @author FTC Team #10273, The Cat in the Hat Comes Back
 */
public class CatHW_Launcher extends CatHW_Subsystem
{

    // Motors:
    public DcMotorEx launcher     = null;
    public Servo stopper   = null;
    public Servo aimer   = null;
    /// TODO make 0.58 a constaint
    private double launchPower = 0.50;
    private boolean isOn = false;
    private double aimerPosition =0.0;
    /* local OpMode members. */
    // Timers:
    private ElapsedTime runtime = new ElapsedTime();


    /* Constructor */
    public CatHW_Launcher(CatHW_Async mainHardware) {
        super(mainHardware);
    }


    /**
     * Initialize standard Hardware interfaces.
     */
    public void init() {

        // Define and Initialize Motors and Servos: //
        launcher        = hwMap.get(DcMotorEx.class, "launcher");
        stopper    = hwMap.servo.get("ring_stopper");
        aimer    = hwMap.servo.get("aimer");

        closeLauncher();

        // Set Motor Directions: //
        launcher.setDirection(DcMotorSimple.Direction.REVERSE);
        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Set Motor and Servo Modes: //
        //launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        //coef = launchWheel.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER);
        RobotConstants.LAUNCH_PID.p = 30;
        RobotConstants.LAUNCH_PID.i = 1;
        RobotConstants.LAUNCH_PID.d = 0;
        launcher.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER,RobotConstants.LAUNCH_PID);

    }



    //----------------------------------------------------------------------------------------------
    // launcher Methods:
    //----------------------------------------------------------------------------------------------

    /**
     *  Turning On and Off the launched wheel
     */

    public void togglePower () {
        if (isOn) {
            powerOff();
        } else {
            powerOn();
        }
    }

    public void powerOn() {
        isOn = true;
        launcher.setPower(launchPower);
    }

    public void updatePower () {
        if (isOn) {
            launcher.setPower(launchPower);
        }
    }

    public void presetPowerShot () {
        launchPower = 0.47;
    }

    public double getLaunchPower () {
        return launchPower;
    }

    public void presetGoal () {
        launchPower = 0.54;
    }

    public void powerOff() {
        isOn = false;
        launcher.setPower(0.0);
    }

    public double getPower () {
        return launchPower;
    }

    public void increasePower () {
        launchPower = launchPower + 0.02;
    }

    public void decreasePower () {
        launchPower = launchPower - 0.02;
    }

    /**
     * Opens and Closes stopper.
     */
    public void closeLauncher () {
        stopper.setPosition(0.45);
    }

    public void openLauncher () { stopper.setPosition(0.7);
    }
    /**
     * right and left for the aimer.
     */
    public void aimL () {
        aimer.setPosition(1.0);
    }

    public void aimR () {
        aimer.setPosition(0.0);
    }

    public void ajustL () {
        aimerPosition = aimerPosition + 0.01;
        if (aimerPosition > 1.0){
            aimerPosition = 1.0;
        }
        aimer.setPosition(aimerPosition);
    }
    public void ajustR () {
        aimerPosition = aimerPosition - 0.01;
        if (aimerPosition < 0.0){
            aimerPosition = 0.0;
        }
        aimer.setPosition(aimerPosition);
    }

    public double getAimerPosition() {
        return aimerPosition;
    }

    //----------------------------------------------------------------------------------------------
    // isDone Method:
    //----------------------------------------------------------------------------------------------
    @Override
    public boolean isDone() {
        Log.d("catbot", String.format("launcher power %.2f,", launcher.getPower()));
        /* isDone stuff for CatHW_Jaws */
        double TIMEOUT = 3.0;
        return !(launcher.isBusy() && (runtime.seconds() < TIMEOUT));
    }
}// End of class bracket