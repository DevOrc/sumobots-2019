package org.inspirerobotics.sumobots.driverstation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.inspirerobotics.sumobots.ComponentState;
import org.inspirerobotics.sumobots.driverstation.network.Connection;
import org.inspirerobotics.sumobots.driverstation.state.DriverstationStateManager;
import org.inspirerobotics.sumobots.driverstation.util.BackendEvent;
import org.inspirerobotics.sumobots.driverstation.util.BackendEventQueue;

import java.util.Optional;
import java.util.function.Supplier;

public class BackendWorker implements Runnable{

    private static final Logger logger = LogManager.getLogger(BackendWorker.class);

    private final DriverstationStateManager stateManager;
    private final Gui gui;

    private Optional<Connection> fieldConnection = Optional.empty();
    private Optional<Connection> robotConnection = Optional.empty();
    private volatile boolean running;

    public BackendWorker(Gui gui) {
        this.gui = gui;
        this.stateManager = new DriverstationStateManager(gui, this);
    }

    public void run(){
        beforeRun();
        runMainLoop();
        shutdown();
    }

    private void beforeRun() {
        running = true;

        logger.info("Backend thread started!");
    }

    private void runMainLoop() {
        while (running){
            runEventsFromEventQueue();

            setFieldConnection(updateConnection(fieldConnection, Connection::createForField));
            setRobotConnection(updateConnection(robotConnection, Connection::createForRobot));
        }
    }

    private void runEventsFromEventQueue() {
        Optional<BackendEvent> e;

        while((e = BackendEventQueue.poll()).isPresent()){
            e.get().run(this);
        }
    }

    private Optional<Connection> updateConnection(Optional<Connection> connection, Supplier<Optional<Connection>> creator){
        if(connection.isPresent()){
            connection.get().update();

            return connection.filter(c -> !c.isClosed());
        }else{
            return creator.get();
        }
    }

    private void shutdown() {
        logger.info("Shutting down backend thread!");
        fieldConnection.ifPresent(Connection::close);
        logger.info("Backend thread shutdown!");
    }

    public void stopRunning(){
        running = false;
    }

    void setFieldConnection(Optional<Connection> fieldConnection) {
        if(fieldConnection != this.fieldConnection){
            this.fieldConnection = fieldConnection;
            stateManager.attemptToChangeComponentState(ComponentState.DISABLED);
            return;
        }

        this.fieldConnection = fieldConnection;
    }

    void setRobotConnection(Optional<Connection> robotConnection) {
        if(robotConnection != this.robotConnection){
            this.robotConnection = robotConnection;
            stateManager.attemptToChangeComponentState(ComponentState.DISABLED);
            return;
        }

        this.robotConnection = robotConnection;
    }

    public Optional<Connection> getRobotConnection() {
        return robotConnection;
    }

    public Optional<Connection> getFieldConnection() {
        return fieldConnection;
    }

    public DriverstationStateManager getStateManager() {
        return stateManager;
    }
}
