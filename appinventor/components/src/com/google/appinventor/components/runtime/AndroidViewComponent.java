// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.SimplePropertyCopier;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;

/**
 * Underlying base class for all components with views; not accessible to Simple
 * programmers.
 * <p>
 * Provides implementations for standard properties and events.
 *
 */
@SimpleObject
public abstract class AndroidViewComponent extends VisibleComponent {

	protected final ComponentContainer container;

	private int percentWidthHolder = LENGTH_UNKNOWN;
	private int percentHeightHolder = LENGTH_UNKNOWN;
	private int lastSetWidth = LENGTH_UNKNOWN;
	private int lastSetHeight = LENGTH_UNKNOWN;

	private int column = ComponentConstants.DEFAULT_ROW_COLUMN;
	private int row = ComponentConstants.DEFAULT_ROW_COLUMN;
	//For padding
	private int padding = 0;
	private int leftPadding = 0;
	private int topPadding = 0;
	private int rightPadding = 0;
	private int bottomPadding = 0;
	// For Margins
	private int margin = 0;
	private int topMargin = 0;
	private int leftMargin = 0;
	private int rightMargin = 0;
	private int bottomMargin = 0;
	
	/**
	 * Creates a new AndroidViewComponent.
	 *
	 * @param container
	 *            container, component will be placed in
	 */
	protected AndroidViewComponent(ComponentContainer container) {
		this.container = container;
	}

	/**
	 * Returns the {@link View} that is displayed in the UI.
	 */
	public abstract View getView();

	/**
	 * Returns true iff the component is visible.
	 * 
	 * @return true iff the component is visible
	 */
	@SimpleProperty(category = PropertyCategory.APPEARANCE)
	public boolean Visible() {
		return getView().getVisibility() == View.VISIBLE;
	}

	/**
	 * Specifies whether the component should be visible on the screen. Value is
	 * true if the component is showing and false if hidden.
	 * 
	 * @param visibility
	 *            desired state
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_VISIBILITY, defaultValue = "True")
	@SimpleProperty(description = "Specifies whether the component should be visible on the screen. "
			+ "Value is true if the component is showing and false if hidden.")
	public void Visible(Boolean visibility) {
		// The principle of least astonishment suggests we not offer the
		// Android option INVISIBLE.
		getView().setVisibility(visibility ? View.VISIBLE : View.GONE);
	}

	/**
	 * Returns the component's horizontal width, measured in pixels.
	 *
	 * @return width in pixels
	 */
	@Override
	@SimpleProperty
	public int Width() {
		int zWidth = (int) (getView().getWidth() / container.$form()
				.deviceDensity());
		// System.err.println("AndroidViewComponent: Width() Called, returning "
		// + zWidth);
		return zWidth;
	}

	/**
	 * Specifies the component's horizontal width, measured in pixels.
	 *
	 * @param width
	 *            in pixels
	 */
	@Override
	@SimpleProperty
	public void Width(int width) {
		container.setChildWidth(this, width);
		lastSetWidth = width;
		if (width <= Component.LENGTH_PERCENT_TAG)
			container.$form().registerPercentLength(this, width,
					Form.PercentStorageRecord.Dim.WIDTH);
	}

	/**
	 * Specifies the component's horizontal width as a percentage of the Width
	 * of its parent Component.
	 *
	 * @param width
	 *            in percent
	 */

	@Override
	@SimpleProperty
	public void WidthPercent(int pCent) {
		if (pCent < 0 || pCent > 100) {
			container.$form().dispatchErrorOccurredEvent(this, "WidthPercent",
					ErrorMessages.ERROR_BAD_PERCENT, pCent);
			return;
		}
		int v = -pCent + Component.LENGTH_PERCENT_TAG;
		Width(v);
	}

	public void setLastWidth(int width) {
		// System.err.println(this + " percentWidthHolder being set to " +
		// width);
		percentWidthHolder = width;
	}

	public int getSetWidth() {
		// System.err.println(this + " getSetWidth() percentWidthHolder = " +
		// percentWidthHolder);
		if (percentWidthHolder == LENGTH_UNKNOWN) {
			return Width(); // best guess...
		} else {
			return percentWidthHolder;
		}
	}

	public void setLastHeight(int height) {
		// System.err.println(this + " percentHeightHolder being set to " +
		// height);
		percentHeightHolder = height;
	}

	public int getSetHeight() {
		// System.err.println(this + " getSetHeight() percentHeightHolder = " +
		// percentHeightHolder);
		if (percentHeightHolder == LENGTH_UNKNOWN) {
			return Height(); // best guess...
		} else {
			return percentHeightHolder;
		}
	}

	/**
	 * Copy the width from another component to this one. Note that we don't use
	 * the getter method to get the property value from the source because the
	 * getter returns the computed width whereas we want the width that it was
	 * last set to. That's because we want to preserve values like
	 * LENGTH_FILL_PARENT and LENGTH_PREFERRED
	 *
	 * @param sourceComponent
	 *            the component to copy from
	 */
	@SimplePropertyCopier
	public void CopyWidth(AndroidViewComponent sourceComponent) {
		Width(sourceComponent.lastSetWidth);
	}

	/**
	 * Returns the component's vertical height, measured in pixels.
	 *
	 * @return height in pixels
	 */
	@Override
	@SimpleProperty
	public int Height() {
		return (int) (getView().getHeight() / container.$form().deviceDensity());
	}

	/**
	 * Specifies the component's vertical height, measured in pixels.
	 *
	 * @param height
	 *            in pixels
	 */
	@Override
	@SimpleProperty
	public void Height(int height) {
		container.setChildHeight(this, height);
		lastSetHeight = height;
		if (height <= Component.LENGTH_PERCENT_TAG)
			container.$form().registerPercentLength(this, height,
					Form.PercentStorageRecord.Dim.HEIGHT);
	}

	/**
	 * Specifies the component's vertical height as a percentage of the height
	 * of its parent Component.
	 *
	 * @param height
	 *            in percent
	 */

	@Override
	@SimpleProperty
	public void HeightPercent(int pCent) {
		if (pCent < 0 || pCent > 100) {
			container.$form().dispatchErrorOccurredEvent(this, "HeightPercent",
					ErrorMessages.ERROR_BAD_PERCENT, pCent);
			return;
		}
		int v = -pCent + Component.LENGTH_PERCENT_TAG;
		Height(v);
	}

	/**
	 * Copy the height from another component to this one. Note that we don't
	 * use the getter method to get the property value from the source because
	 * the getter returns the computed width whereas we want the width that it
	 * was last set to. That's because we want to preserve values like
	 * LENGTH_FILL_PARENT and LENGTH_PREFERRED
	 *
	 * @param sourceComponent
	 *            the component to copy from
	 */
	@SimplePropertyCopier
	public void CopyHeight(AndroidViewComponent sourceComponent) {
		Height(sourceComponent.lastSetHeight);
	}

	/**
	 * Column property getter method.
	 *
	 * @return column property used by the table arrangement
	 */
	@SimpleProperty(userVisible = false)
	public int Column() {
		return column;
	}

	/**
	 * Column property setter method.
	 *
	 * @param column
	 *            column property used by the table arrangement
	 */
	@SimpleProperty(userVisible = false)
	public void Column(int column) {
		this.column = column;
	}

	/**
	 * Row property getter method.
	 *
	 * @return row property used by the table arrangement
	 */
	@SimpleProperty(userVisible = false)
	public int Row() {
		return row;
	}

	/**
	 * Row property setter method.
	 *
	 * @param row
	 *            row property used by the table arrangement
	 */
	@SimpleProperty(userVisible = false)
	public void Row(int row) {
		this.row = row;
	}

	// Component implementation

	@Override
	public HandlesEventDispatching getDispatchDelegate() {
		return container.$form();
	}

	
	// Padding
//	@SimpleProperty(category = PropertyCategory.APPEARANCE)
//	public int LeftPadding() {
//		return LeftPadding;
//	}
//
//	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = Component.MARGIN_DEFAULT)
//    @SimpleProperty(description = "Returns the component's left padding")
//	public void LeftPadding(int leftPadding) {
//		LeftPadding = leftPadding;
//		setPadding(leftPadding, TopPadding, RightPadding, BottomPadding);
//	}
//
//	@SimpleProperty(category = PropertyCategory.APPEARANCE)
//	public int TopPadding() {
//		return TopPadding;
//	}
//
//	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = Component.MARGIN_DEFAULT)
//    @SimpleProperty(description = "Returns the component's top padding")
//	public void TopPadding(int topPadding) {
//		TopPadding = topPadding;
//		setPadding(LeftPadding, topPadding, RightPadding, BottomPadding);
//	}
//
//	@SimpleProperty(category = PropertyCategory.APPEARANCE)
//	public int RightPadding() {
//		return RightPadding;
//	}
//
//	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = Component.MARGIN_DEFAULT)
//    @SimpleProperty(description = "Returns the component's right padding")
//	public void RightPadding(int rightPadding) {
//		RightPadding = rightPadding;
//		setPadding(LeftPadding, TopPadding, rightPadding, BottomPadding);
//	}
//	
//	@SimpleProperty(category = PropertyCategory.APPEARANCE)
//	public int BottomPadding() {
//		return BottomPadding;
//	}
//
//	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = Component.MARGIN_DEFAULT)
//    @SimpleProperty(description = "Returns the component's bottom padding")
//	public void BottomPadding(int bottomPadding) {
//		BottomPadding = bottomPadding;
//		setPadding(LeftPadding, TopPadding, RightPadding, bottomPadding);
//	}
	
	
	
//	private void setPadding(int padding) {
//		if(leftPadding !=0 || topPadding !=0 || rightPadding !=0 || bottomPadding !=0){
//			getView().setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
//			getView().invalidate();
//		} else if(padding != 0){}
//		getView().setPadding(padding, padding, padding, padding);
//		getView().invalidate();
//		getView().setPadding(padding, padding, padding, padding);
//	}

	@SimpleProperty(category = PropertyCategory.APPEARANCE, 
			description = "Returns the component's padding, if set to be equal in all directions.")
	public int Padding() {
		return padding;
	}

	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
	@SimpleProperty(description = "Specifies the component's padding, equal on all sides. ")
	public void Padding(int value) {
		this.padding = value;
		setPaddings(value);
	}

	@SimpleFunction(description = "Get a component's left padding.")
	public int LeftPadding() {
		return leftPadding;
	}

	@SimpleFunction(description = "Set a component's left padding.")
	public void LeftPadding(int leftPadding) {
		this.leftPadding = leftPadding;
		setPaddings(leftPadding);
	}

	@SimpleFunction(description = "Get a component's top padding.")
	public int TopPadding() {
		return topPadding;
	}

	@SimpleFunction(description = "Set a component's top margin.")
	public void TopPadding(int topPadding) {
		this.topPadding = topPadding;
		setPaddings(topPadding);
	}

	@SimpleFunction(description = "Get a component's right padding.")	
	public int RightPadding() {
		return rightPadding;
	}

	@SimpleFunction(description = "Set a component's right padding.")
	public void RightPadding(int rightPadding) {
		this.rightPadding = rightPadding;
		setPaddings(rightPadding);
	}

	@SimpleFunction(description = "Get a component's bottom padding.")
	public int BottomPadding() {
		return bottomPadding;
	}

	@SimpleFunction(description = "Set a component's bottom padding.")
	public void BottomPadding(int bottomPadding) {
		this.bottomPadding = bottomPadding;
		setPaddings(rightPadding);
	}	
	
	private void setPaddings(int padding) {
		

			if(leftPadding != 0 || topPadding != 0 || rightPadding != 0
					|| bottomPadding != 0){

				getView().setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
				getView().invalidate();
			}
			else {
				leftPadding = padding;
				topPadding = padding;
				rightPadding = padding;
				bottomPadding = padding;
				getView().setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
				getView().invalidate();
			}				
	}

	


	@SimpleProperty(category = PropertyCategory.APPEARANCE,
			description = "Returns the component's margins, if set and all-equal.")
	public int Margin() {
		return margin;
	}

	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
	@SimpleProperty(description = "Specifies the component's all-equal margins. ")
	public void Margin(int value) {
		this.margin = value;
		setMargins(value);
	}

//Atm I lack better idea how to solve margins TT

	private void setMargins(int margin) {
		
		android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
				getView().getLayoutParams());
				//getLayoutManager().getLayoutParams());

			if(leftMargin != 0 || topMargin != 0 || rightMargin != 0
					|| bottomMargin != 0){

				params.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
				getView().setLayoutParams(params);
				
			}
			else {
				params.setMargins(margin, margin, margin, margin);
				getView().setLayoutParams(params);
			}
			getView().invalidate();				
	}

	@SimpleFunction(description = "Get a component's top margin.")
	public int TopMargin() {
		return topMargin;
	}

	@SimpleFunction(description = "Set a component's top margin.")
	public void TopMargin(int topMargin) {
		this.topMargin = topMargin;
		setMargins(topMargin);
		//viewLayout.getLayoutManager().
	}

	@SimpleFunction(description = "Get a component's left margin. ")
	public int LeftMargin() {
		return leftMargin;
	}

	@SimpleFunction(description = "Set a component's left margin.")
	public void LeftMargin(int leftMargin) {
		this.leftMargin = leftMargin;
		setMargins(leftMargin);
	}

	@SimpleFunction(description = "Get a component's left margin. ")
	public int RightMargin() {
		return rightMargin;
	}

	@SimpleFunction(description = "Set a component's right margin.")
	public void RightMargin(int rightMargin) {
		this.rightMargin = rightMargin;
		setMargins(rightMargin);
	}

	@SimpleFunction(description = "Get a component's bottom margin. ")
	public int BottomMargin() {
		return bottomMargin;
	}

	@SimpleFunction(description = "Set a component's bottom margin.")
	public void BottomMargin(int bottomMargin) {
		this.bottomMargin = bottomMargin;
		setMargins(bottomMargin);
	}
	
}
