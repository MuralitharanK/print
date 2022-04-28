package io.mosip.print.model;

import lombok.Data;

/**
 * Rectangle model for pdf generator
 * 
 * @author Urvil Joshi
 *
 */
@Data
public class Rectangle {

	public Rectangle(float llx, float lly, float urx, float ury) {
		this.llx = llx;
		this.lly = lly;
		this.urx = urx;
		this.ury = ury;
	}
	/**
	 * The lower left x value of rectangle.
	 */
	private float llx;
	/**
	 * The lower left y value of rectangle.
	 */
	private float lly;
	/**
	 * The upper right x value of rectangle.
	 */
	private float urx;
	/**
	 * The upper right y value of rectangle.
	 */
	private float ury;

}