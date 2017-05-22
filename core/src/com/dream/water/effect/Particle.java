package com.dream.water.effect;

import com.badlogic.gdx.math.Vector2;

/**
 * Particle representing a drop of water formed when a body impacts against water
 */
public class Particle {
	
	private Vector2 position;
	private Vector2 velocity;
	private float radius;
	private float time;
	private float initX;
	
	/**
	 * Main constructor
	 * @param position Position where the particle will be created
	 * @param velocity Initial velocity of the particle
	 * @param radius Radius of the texture
	 */
	public Particle(Vector2 position, Vector2 velocity, float radius){
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

	public float getRadius() {
		return radius;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

}