/*
 * Copyright (C) 2017 John Garner <segfaultcoredump@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.timing.reader;

import com.pikatimer.PikaPreferences;
import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.controlsfx.control.ToggleSwitch;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public abstract class TailingReader implements TimingReader{
        
    protected TimingListener timingListener;
    protected File sourceFile; 
    protected final StringProperty fileName; 
    protected Pane displayPane; 
    private Button inputButton;
    protected TextField inputTextField; 
    protected Label statusLabel; 
    private HBox displayHBox; 
    protected VBox displayVBox; 
    protected Tailer tailer;
    private Thread tailingThread; 
    private Thread readingThread;
    protected final BooleanProperty readingStatus;
    ProgressIndicator watchProgressIndicator;
    ToggleSwitch autoImportToggleSwitch;
    private Semaphore reading = new Semaphore(1);

    
    public TailingReader(){
        fileName = new SimpleStringProperty();
        readingStatus = new SimpleBooleanProperty();
        
    }



    public void selectInput() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        
        if (sourceFile != null && sourceFile.exists()) {
            fileChooser.setInitialDirectory(sourceFile.getParentFile()); 
            fileChooser.setInitialFileName(sourceFile.getName());
        } else {
            fileChooser.setInitialDirectory(PikaPreferences.getInstance().getCWD()); 
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        
        sourceFile = fileChooser.showOpenDialog(inputButton.getScene().getWindow());
        if (sourceFile != null) {
            // if we are auto-importing, stop that
            readingStatus.set(false);
            
            fileName.setValue(sourceFile.getAbsolutePath());
            // save the filename 
            timingListener.setAttribute("TailingReader:filename", sourceFile.getAbsolutePath());
            
            // set the text field to the filename
            inputTextField.textProperty().setValue(fileName.getValueSafe());
            // read the file
            if (!sourceFile.canRead()){
                statusLabel.setText("Unable to open file: " + fileName.getValueSafe());
            } else readOnce();
        }                
    }

    

    @Override
    public void startReading() {
        System.out.println("TailingReader:StartReading() called");
        if (tailingThread != null && tailingThread.isAlive()) return;
        if (readingThread != null && readingThread.isAlive()) return;
        Task readingTask = new Task<Void>() {
                @Override public Void call() {
                    try {
                        // make sure the file exists

                        
                        while (readingStatus.getValue() && (sourceFile == null || !sourceFile.exists() || !sourceFile.canRead() || !sourceFile.isFile())){
                            Thread.sleep(1000);
                            System.out.println("Waiting for " + sourceFile.getPath());
                            Platform.runLater(() ->{
                                statusLabel.setText("Waiting for " + sourceFile.getPath());
                            });
                        }   
                        if (readingStatus.getValue() ) {
                            Platform.runLater(() -> statusLabel.setText("Reading file: " + sourceFile.getPath()));

                            MyHandler listener = new MyHandler();
                            tailer = new Tailer(sourceFile, listener, 1000, Boolean.FALSE, Boolean.TRUE);
                            tailingThread = new Thread(tailer);
                            tailingThread.setDaemon(true); // optional
                            tailingThread.start();
                            readingStatus.setValue(Boolean.TRUE);
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TailingReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return null;
                }
        };
        readingThread = new Thread(readingTask);
        readingThread.setDaemon(true); // optional
        readingThread.start();
    }
    
    @Override
    public void stopReading() {
        if (tailer != null) {
            tailer.stop();
        }
        readingStatus.setValue(Boolean.FALSE);
    }


    @Override
    public void showControls(Pane p) {
        
        if (displayPane == null) {
            // initialize our display
            displayHBox = new HBox();
            displayVBox = new VBox();
            watchProgressIndicator = new ProgressIndicator();
            autoImportToggleSwitch = new ToggleSwitch("Auto-Import File");
            autoImportToggleSwitch.selectedProperty().set(false);
            autoImportToggleSwitch.setPadding(new Insets(3, 0, 0, 0)); // this is a hack to get around a ToggleSwitch bug
            //autoImportToggleSwitch.setMaxWidth(75);
            statusLabel = new Label("");
            inputButton = new Button("Select File...");
            inputTextField = new TextField();
            displayVBox.setSpacing(5); 
            //displayVBox.setPadding(new Insets(5, 5, 5, 5));
            
            
            inputTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                if (!newPropertyValue && !fileName.getValueSafe().equals(inputTextField.textProperty().getValueSafe())) {
                    // if we are auto-importing, stop that
                    stopReading();
                    
                    sourceFile = new File(inputTextField.textProperty().getValueSafe()).getAbsoluteFile();
                    fileName.setValue(sourceFile.getAbsolutePath());

                    
                    // save the filename 
                    timingListener.setAttribute("TailingReader:filename", inputTextField.textProperty().getValueSafe());

                    // read the file
                    if (!sourceFile.canRead()){
                        statusLabel.setText("Unable to open file: " + fileName.getValueSafe());
                    } else readOnce();
                        
                } else {
                    System.out.println("No change in file name");
                }
            });
            
            displayHBox.setSpacing(5);
            displayHBox.setAlignment(Pos.CENTER_LEFT);
            displayHBox.getChildren().addAll(inputTextField, inputButton, autoImportToggleSwitch, watchProgressIndicator); 
            displayVBox.getChildren().addAll(displayHBox, statusLabel); 
            
            // Set the action for the inputButton
            inputButton.setOnAction((event) -> {
                // Button was clicked, do something...
                selectInput();
            });
            
            watchProgressIndicator.visibleProperty().bind(autoImportToggleSwitch.selectedProperty());
            watchProgressIndicator.setProgress(-1.0);
            // get the current status of the reader
            //watchProgressIndicator.setPrefHeight(30.0);
            watchProgressIndicator.setMaxHeight(30.0);
            autoImportToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(newValue) {
                    System.out.println("TailingReader: autoImportToggleSwitch event: calling startReading()");
                    startReading();
                } else {
                    System.out.println("TailingReader: autoImportToggleSwitch event: calling stopReading()");
                    stopReading();
                }
            });
            autoImportToggleSwitch.selectedProperty().bindBidirectional(readingStatus);
            
            inputTextField.textProperty().setValue(fileName.getValueSafe());
            // set the action for the inputTextField
            
        }
        
        // If we were previously visible, clear the old one
        if (displayPane != null) displayPane.getChildren().clear();
        
        // Now show ourselves.... 
        displayPane = p; 
        displayPane.getChildren().clear();
        displayPane.getChildren().add(displayVBox); 
        
        
    }

    @Override
    public BooleanProperty getReadingStatus() {
        return readingStatus; 
    }
    
    private class MyHandler extends TailerListenerAdapter {
        @Override
        public void handle(String line) {
            System.out.println("handle: " + line);
            process(line);
        }
    }
    
    
    public abstract void process(String s);
        
    @Override
    public void readOnce() {
        // get the event date, just in case we need it
        System.out.println("TailingReader.readOnce called.");
        stopReading();
        
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.canRead() || !sourceFile.isFile()) {
            statusLabel.setText("Unable to open file: " + fileName.getValueSafe());
            return;
        }
        System.out.println("  Current file is: \"" + sourceFile.getAbsolutePath() + "\"");
        // Run this in a tailingThread....
        Task task;
        task = new Task<Void>() {
            @Override public Void call() {
                try {
                    reading.acquire();
                    try (Stream<String> s = Files.lines(sourceFile.toPath())) {
                        s.map(line -> line.trim()).filter(line -> !line.isEmpty()).forEach(line -> {
                            //System.out.println("readOnce read " + s);
                            process(line);
                        });
                        s.close();
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                    
                } catch (Exception ex){
                    ex.printStackTrace();
                }
                reading.release();
                return null;
            }
        };
        new Thread(task).start();
        
    }
    
    

    @Override
    public void setTimingListener(TimingListener t) {
        timingListener = t; 
        
        // get any existing attributes
        String filename = timingListener.getAttribute("TailingReader:filename");
        if (filename != null) {
            System.out.println("TailingReader: Found existing file setting: " + filename);
            sourceFile = new File(filename).getAbsoluteFile();
            fileName.setValue(filename);
            
        } else {
            System.out.println("TailingReader: Did not find existing file setting." );
        }
        
        
    }
    
//    @Override
//    public Boolean chipIsBib() {
//        return Boolean.FALSE; 
//    }

}
