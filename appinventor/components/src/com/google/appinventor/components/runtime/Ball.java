// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.PaintUtil;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Simple ball, based on Sprite implementation.
 *
 */
@DesignerComponent(version = YaVersion.BALL_COMPONENT_VERSION,
    description = "<p>A round 'sprite' that can be placed on a " +
    "<code>Canvas</code>, where it can react to touches and drags, " +
    "interact with other sprites (<code>ImageSprite</code>s and other " +
    "<code>Ball</code>s) and the edge of the Canvas, and move according " +
    "to its property values.</p>" +
    "<p>For example, to have a <code>Ball</code> move 4 pixels toward the " +
    "top of a <code>Canvas</code> every 500 milliseconds (half second), " +
    "you would set the <code>Speed</code> property to 4 [pixels], the " +
    "<code>Interval</code> property to 500 [milliseconds], the " +
    "<code>Heading</code> property to 90 [degrees], and the " +
    "<code>Enabled</code> property to <code>True</code>.  These and its " +
    "other properties can be changed at any time.</p>" +
    "<p>The difference between a Ball and an <code>ImageSprite</code> is " +
    "that the latter can get its appearance from an image file, while a " +
    "Ball's appearance can only be changed by varying its " +
    "<code>PaintColor</code> and <code>Radius</code> properties.</p>",
    category = ComponentCategory.ANIMATION)
@SimpleObject
public final class Ball extends Sprite {
	
 private final Activity context;
 private final Form form;
  private int radius;
  private int paintColor;
  private Paint paint;
  static final int DEFAULT_RADIUS = 5;
  private static final float DEFAULT_LINE_WIDTH = 2;
  private static final int DEFAULT_PAINT_COLOR = Component.COLOR_BLACK;
  private boolean Fill;

  public Ball(ComponentContainer container) {
    super(container);
  context = container.$context();
  form = container.$form();
    
    paint = new Paint();

    // Set default properties.
    PaintColor(Component.COLOR_BLACK);
    Radius(DEFAULT_RADIUS);
    Fill(true);
  }

  // Implement or override methods

  //@Override
  public Activity $context() {
    return context;
  }

//  @Override
  public Form $form() {
    return form;
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    if (visible) {
      float correctedXLeft = (float)(xLeft * form.deviceDensity());
      float correctedYTop =  (float)(yTop * form.deviceDensity());
      float correctedRadius = radius * form.deviceDensity();
      Paint p = new Paint(paint);
      p.setStyle(Fill ? Paint.Style.FILL : Paint.Style.STROKE);
      canvas.drawCircle(correctedXLeft + correctedRadius, correctedYTop +
          correctedRadius, correctedRadius, p);
    }
  }

  // The following four methods are required by abstract superclass
  // VisibleComponent.  Because we don't want to expose them to the Simple
  // programmer, we omit the SimpleProperty and DesignerProperty pragmas.
  @Override
  public int Height() {
    return 2 * radius;
  }

  @Override
  public void Height(int height) {
    // ignored
  }

  @Override
  public void HeightPercent(int pCent) {
    // ignored
  }

  @Override
  public int Width() {
    return 2 * radius;
  }

  @Override
  public void Width(int width) {
    // ignored
  }

  @Override
  public void WidthPercent(int pCent) {
    // ignored
  }

  @Override
  public boolean containsPoint(double qx, double qy) {
    double xCenter = xLeft + radius;
    double yCenter = yTop + radius;
    return ((qx - xCenter) * (qx - xCenter) + (qy - yCenter) * (qy - yCenter))
        <= radius * radius;
  }


  // Additional properties

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
      defaultValue = "5")
  @SimpleProperty(
      // Kind of both categories: APPEARANCE and BEHAVIOR
      category = PropertyCategory.APPEARANCE)
  public void Radius(int radius) {
    this.radius = radius;
    registerChange();
  }

  @SimpleProperty
  public int Radius() {
    return radius;
  }

  /**
   * PaintColor property getter method.
   *
   * @return  paint RGB color with alpha
   */
  @SimpleProperty(
      category = PropertyCategory.APPEARANCE)
  public int PaintColor() {
    return paintColor;
  }

  /**
   * PaintColor property setter method.
   *
   * @param argb  paint RGB color with alpha
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
      defaultValue = Component.DEFAULT_VALUE_COLOR_BLACK)
  @SimpleProperty
  public void PaintColor(int argb) {
    paintColor = argb;
    if (argb != Component.COLOR_DEFAULT) {
      PaintUtil.changePaint(paint, argb);
    } else {
      // The default paint color is black.
      PaintUtil.changePaint(paint, Component.COLOR_BLACK);
    }
    registerChange();
  }
  
  /**
   * Returns the currently specified stroke width
   * @return width
   */
  @SimpleProperty(
      description = "The width of line.",
      category = PropertyCategory.APPEARANCE)
  public float LineWidth() {
    return paint.getStrokeWidth() / $form().deviceDensity();
  }

  /**
   * Specifies the stroke width
   *
   * @param width
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT,
      defaultValue = DEFAULT_LINE_WIDTH + "")
  @SimpleProperty
  public void LineWidth(float width) {
    paint.setStrokeWidth(width * $form().deviceDensity());
  }
  
  /**
   * Returns true if the ball is to be filled
   *
   * @return {@code true} indicates that ball will be filled, {@code false} will not
   */
  @SimpleProperty(
    category = PropertyCategory.BEHAVIOR)
  public boolean Fill() {
    return Fill;
  }

  /**
   * Specifies whether the will be filled
   *
   * @param front
   *          {@code true} for front-facing camera, {@code false} for default
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
  @SimpleProperty(description = "Specifies whether ball will be filled or not. "
    + "If true, which is default value, it will be filled. "
    + "Otherwise it will be circle.")
  public void Fill (boolean newValue) {
    Fill = newValue;
  }
  
}
