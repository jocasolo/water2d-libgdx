package com.dream.water.effect;

public class WaterColumn {

	private float targetHeight;
	private float height;
	private float speed;
	private float x, y;
	private long startTime;
	
	public WaterColumn(float x, float y, float targetHeight, float height, float speed) {
		this.targetHeight = targetHeight;
		this.height = height;
		this.speed = speed;
		this.x(x);
		this.y(y);
		
		this.startTime = 0;
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

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
}
