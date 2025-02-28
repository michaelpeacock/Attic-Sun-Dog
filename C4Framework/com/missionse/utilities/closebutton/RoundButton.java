package com.missionse.utilities.closebutton;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.SVGPathBuilder;
import javafx.scene.text.TextBuilder;
import javafx.stage.Stage;

/** 
 * Sample code for: https://forums.oracle.com/forums/thread.jspa?threadID=2428756 "Problem with a circle shaped border around a button"
 * 
 * Demonstrates three different methods of creating an icon in a scalable close button.
 */
public class RoundButton extends Application {
  public static void main(String[] args) { launch(args); }
  private DoubleBinding scaleFactor;
  @Override public void start(Stage primaryStage) {
    EventHandler<ActionEvent> exitHandler = new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent event) {
        Platform.exit();
      }
    };  
    
    HBox layout = new HBox(10);
    layout.getChildren().addAll(
    		closeButton(textCloseIcon(),    exitHandler),
      closeButton(textCloseIcon(),    exitHandler),
      closeButton(pathCloseIcon(),    exitHandler),
      closeButton(svgPathCloseIcon(), exitHandler)
    );
    
    layout.getStylesheets().add(this.getClass().getResource("roundbutton.css").toExternalForm());
    primaryStage.setScene(new Scene(layout));
    primaryStage.show();
  }

  public Region closeButton(Node closeIcon, EventHandler<ActionEvent> actionHandler) {
    final double BUTTON_SIZE = 30;
    
    StackPane closeButton = new StackPane();
    closeButton.getStyleClass().add("closebutton");
    Button button = new Button();
    button.setOnAction(actionHandler);
    
    closeIcon.setMouseTransparent(true);

    closeButton.getChildren().addAll(button, closeIcon);
    button.setMinSize(BUTTON_SIZE, BUTTON_SIZE);
    closeButton.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
    closeButton.setMaxSize(StackPane.USE_PREF_SIZE, StackPane.USE_PREF_SIZE);
    return closeButton;
  }
  
  public Node textCloseIcon() {
    return TextBuilder.create()
      .text("X")
      .styleClass("closetext")
      .build();
  }

  private Node pathCloseIcon() {
    Path path = new Path();
    path.getElements().addAll(
      new MoveTo( 9,  9),      
      new LineTo(21, 21),
      new MoveTo( 9, 21),
      new LineTo(21,  9)
    );
    path.getStyleClass().add("closeicon");
    
    return path;
  }
  
  private Node svgPathCloseIcon() {
    return SVGPathBuilder.create()
      .content("M9 9 L21 21 M9 21 L21 9")
      .styleClass("closeicon")
      .build();
  }
  
  private Group scaleTo(double size, Region n) {
    Group g = new Group(n);
    scaleFactor = (new SimpleDoubleProperty(size)).divide(n.widthProperty());

    n.scaleXProperty().bind(scaleFactor);
    n.scaleYProperty().bind(scaleFactor);
    
    return g;
  }
}