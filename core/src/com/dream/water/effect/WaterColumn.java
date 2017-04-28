package com.dream.water.effect;

public class WaterColumn {

	private float targetHeight;
	private float height;
	private float speed;
	
	public WaterColumn(float targetHeight, float height, float speed) {
		this.targetHeight = targetHeight;
		this.height = height;
		this.speed = speed;
	}
	
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
	
}
