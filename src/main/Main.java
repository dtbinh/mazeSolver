package main;

import console.LoggerProvider;
import dataTransmitter.GridDataTransmitter;
import display.EnterInformation;
import head.OnlyFirstSense;
import head.Sense;
import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.SensorPort;
import lejos.nxt.TouchSensor;
import lejos.robotics.objectdetection.Feature;
import lejos.robotics.objectdetection.FeatureDetector;
import lejos.robotics.objectdetection.FeatureListener;
import lejos.robotics.objectdetection.TouchFeatureDetector;
import localization.grid.GridWorld;
import localization.grid.PositionInGrid;
import localization.grid.aStar.AStarGridWorld;
import localization.maze.Direction;
import localization.maze.DirectionalPoint;
import localization.maze.MazePoint;
import movement.FakeRunner;
import movement.GridWorldRunner;
import movement.MovementInMaze;
import movement.MovementTest;

import java.util.Arrays;

/**
 * Entry point.
 * Date: 8/11/12
 */
public class Main {
    private static boolean debugMode;

    public static void main(String[] args) throws InterruptedException {
        debugMode = args != null && args.length > 0;

        assertHit();
        mazeSolve(args);

//        testMovement();
//        loggerTest();

//        sensorTest();
    }

    private static void testMovement(){
        GridDataTransmitter.initTransmitter(false);
        LoggerProvider.initProvider(false, false);

        MovementTest.test();
    }

    private static void assertHit(){
        if (debugMode){
            return;
        }

        FeatureDetector fd1 = new TouchFeatureDetector(new TouchSensor(SensorPort.S2));
        FeatureDetector fd2 = new TouchFeatureDetector(new TouchSensor(SensorPort.S1));

        FeatureListener listener = new FeatureListener() {
            @Override
            public void featureDetected(Feature feature, FeatureDetector detector) {
                throw new RuntimeException("Hit something");
            }
        };
        fd1.addListener(listener);
        fd2.addListener(listener);
    }

    private static void loggerTest(){
        LoggerProvider.initProvider(false, false);

        Sense sense = new Sense();
        sense.senseIteration();

        LoggerProvider.sendMessage("Left: " + sense.getLeft());
        LoggerProvider.sendMessage("Right: " + sense.getRight());
        LoggerProvider.sendMessage("Front: " + sense.getFront());

        LoggerProvider.closeLogger();
        System.out.println("Press any key again");
        Button.waitForAnyPress();
    }

    private static void mazeSolve(String[] args) {
        if (!debugMode){
            LoggerProvider.initProvider(debugMode);
            GridDataTransmitter.initTransmitter();

            EnterInformation.showGreetings();
            EnterInformation.setupFeatures();
        }

        MazePoint goal;

        if (debugMode) {
            goal = new MazePoint(Double.valueOf(args[0]), Double.valueOf(args[1]));
        } else {
            goal = EnterInformation.enter();
            if (goal == null) {
                System.out.println("Canceled");
                Button.waitForAnyPress();
                return;
            }
        }

        LoggerProvider.sendMessage("I will go to:\n" + goal);
        System.out.print("I will go to:\n" + goal);
        if (!debugMode) {
            Button.waitForAnyPress();
        }
        MovementInMaze movementInMaze;
        if (debugMode) {
            int[][] expectedGrid = new int[5][];
            expectedGrid[4] = new int[]{-2};
            expectedGrid[3] = new int[]{-2};
            expectedGrid[2] = new int[]{-2};
            expectedGrid[1] = new int[]{-2};
            expectedGrid[0] = new int[]{-2};

            movementInMaze = new FakeRunner(expectedGrid);
        } else {
            movementInMaze = new MovementInMaze();
        }
        GridWorldRunner runner = new GridWorldRunner(goal, movementInMaze);
        runner.run();

        if (!debugMode) {
            LCD.drawString("Complited", 0, 4);
            GridDataTransmitter.sendFinishedMessage("Complited!");
            Button.waitForAnyPress();
        }
    }

    /**
     * Entry point for testing usage of clustering,
     * based on many sonic sensor's readings
     */
    private static void sensorTest() throws InterruptedException {
        GridDataTransmitter.initTransmitter(true);
        LoggerProvider.initProvider(false, false);

        Sense sense = new OnlyFirstSense();

        int count = 5;

        int[] fronts = new int[count];
        int minimumDistance;

        GridWorld world = new AStarGridWorld(new MazePoint(4, 4));
        PositionInGrid position = new PositionInGrid();
        position.setCurrentPosition(new DirectionalPoint(Direction.FRONT, 2, 0));

        for (int i=0; i<count; i++){
            sense.senseIteration();
            minimumDistance = sense.getFront();
            fronts[i] = minimumDistance;
            position.setFrontMeasure(GridWorld.getDistanceInCells(minimumDistance));
            world.actualize(position);
        }

        GridDataTransmitter.convertAndSendData(world.getGrid());
        GridDataTransmitter.sendFinishedMessage("fronts: " + Arrays.toString(fronts));
    }
}
