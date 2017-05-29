package com.dream.box2d.water;

import com.badlogic.gdx.physics.box2d.Body;

/**
 * Allows to create a column represented by a line that will serve to simulate the height of a wave
 */
public class WaterColumn {

	private float targetHeight;
	private float height;
	private float speed;
	private float x, y;
	private Body actualBody; // Body in contact with this column
	
	/**
	 * Main constructor. 
	 * @param x Position on the x-axis of the lower point of the column
	 * @param y Position on the y-axis of the lower point of the column
	 * @param targetHeight Default height of the column
	 * @param height Actual height of the column
	 * @param speed Speed at which it is currently oscillating
	 */
	public WaterColumn(float x, float y, float targetHeight, float height, float speed) {
		this.targetHeight = targetHeight;
		this.height = height;
		this.speed = speed;
		this.x(x);
		this.y(y);
	}
	
	/**
	 * Updates the current height of the column with respect to its speed
	 * @param dampening Dampening value
	 * @param tension Tension value
	 */
	public void update(float dampening, float tension){
		float x = targetHeight - height;
		speed += tension * x - speed * dampening;
		height += speed;
	}
	
	public float getTargetHeight() {
		return targetHeight;
	}
	
	public void setTargetHeight(float targetHeight) {
		this.targetHeight = targetHeight;
	}
	
	public float getHeight() {
		return height;
	}
	
	public void setHeight(float height) {
		this.height = height;
	}
	
	public float getSpeed() {
		return speed;
	}
	
	public void setSpeed(float speed) {
		this.speed = speed;
	}
	
	public float x() {
		return x;
	}
	
	public void x(float x) {
		this.x = x;
	}
	
	public float y() {
		return y;
	}
	
	public void y(float y) {
		this.y = y;
	}

	public Body getActualBody() {
		return actualBody;
	}

	public void setActualBody(Body actualBody) {
		this.actualBody = actualBody;
	}
	
}
