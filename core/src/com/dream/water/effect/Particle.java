package com.dream.water.effect;

import com.badlogic.gdx.math.Vector2;

public class Particle {
	
	private Vector2 position;
	private Vector2 velocity;
	private float time;
	private float initX;
	
	public Particle(Vector2 position, Vector2 velocity, float orientation){
		this.position = position;
		this.velocity = velocity;
	}
	
	public Vector2 getPosition() {
		return position;
	}
	
	public void setPosition(Vector2 position) {
		this.position = position;
	}
	
	public Vector2 getVelocity() {
		return velocity;
	}
	
	public void setVelocity(Vector2 velocity) {
		this.velocity = velocity;
	}

	public float getTime() {
		return time;
	}

	public void setTime(float time) {
		this.time = time;
	}

	public float getInitX() {
		return initX;
	}

	public void setInitX(float initX) {
		this.initX = initX;
	}

}