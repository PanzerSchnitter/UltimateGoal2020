package org.firstinspires.ftc.teamcode;


import android.util.Log;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;


/**
 * CatHW_Jaws.java
 *
 *
 * This class containing common code accessing hardware specific to the movement of the jaws/intake.
 *
 * This is NOT an OpMode.  This class is used to define all the other hardware classes.
 * This hardware class assumes the following device names have been configured on the robot.
 *
 *
 * @author FTC Team #10273, The Cat in the Hat Comes Back
 */
public class CatHW_Jaws extends CatHW_Subsystem
{

    // Motors: //
    public DcMotor intakeMotor = null;
    public DcMotor transferMotor = null;
    public Servo leftArm = null;
    public Servo rightArm = null;



    // Timers: //
    private ElapsedTime runtime = new ElapsedTime();


    /* Constructor */
    public CatHW_Jaws(CatHW_Async mainHardware) {
        super(mainHardware);

    }


    /* Initialize standard Hardware interfaces */
    public void init() {

        // Define and initialize motors: //
        intakeMotor = hwMap.dcMotor.get("intake");
        intakeMotor.setDirection(DcMotorSimple.Direction.FORWARD);

        // Set motor modes: //
        intakeMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        transferMotor = hwMap.dcMotor.get("transfer");
        transferMotor.setDirection(DcMotorSimple.Direction.FORWARD);

        // Set motor modes: //
        transferMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        leftArm = hwMap.get(Servo.class,"leftArm");
        rightArm = hwMap.get(Servo.class,"rightArm");
        leftArm.setPosition(0);
        rightArm.setPosition(.75);


    }


    //----------------------------------------------------------------------------------------------
    // Jaw Methods:
    //----------------------------------------------------------------------------------------------

    /**
     * Set the power of both jaw motors.
     *
     * @param power at which the motors will spin.
     */
    public void setJawPower(double power) {
        intakeMotor.setPower(power);
    }
    public double getJawPower() {
        return intakeMotor.getPower();
    }

    /**
     * Turn off both jaws motors.
     */
    public void turnOffJaws() {
        intakeMotor.setPower(0.0);
    }


    //----------------------------------------------------------------------------------------------
    // transfer Methods:
    //----------------------------------------------------------------------------------------------

    /**
     * Set the power of both transfer motors.
     *
     * @param power at which the motors will spin.
     */
    public void setTransferPower(double power) {
        transferMotor.setPower(power);
    }


    /**
     * Turn off both Transfer motors.
     */
    public void turnOffTransfer() {
        transferMotor.setPower(0.0);
    }

    //----------------------------------------------------------------------------------------------
    // isDone Method:
    //----------------------------------------------------------------------------------------------
    @Override
    public boolean isDone() {
        Log.d("catbot", String.format(" intake power %.2f,", transferMotor.getPower()));
        /* isDone stuff for CatHW_Jaws */
        double TIMEOUT = 3.0;
        return !(transferMotor.isBusy() && (runtime.seconds() < TIMEOUT));
    }
}