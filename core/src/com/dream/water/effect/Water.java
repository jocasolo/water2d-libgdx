package com.dream.water.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Predicate;

public class Water {

	private List<WaterColumn> columns = new ArrayList<WaterColumn>(201);
	List<Particle> particles = new ArrayList<Particle>();

	private static Random rand = new Random();

	private final float tension = 0.025f;
	private final float dampening = 0.025f;
	private final float spread = 0.25f;

	Predicate<Particle> predicate;

	public Water() {
		
		predicate = new Predicate<Particle>() {
			@Override
			public boolean evaluate(Particle p) {
				return p.getPosition().x >= 0 && p.getPosition().x <= 800
						&& p.getPosition().y - 5 <= getHeight(p.getPosition().x); // ******************
			}
		};
		
		for (int i = 0; i < columns.size(); i++) {
			columns.add(new WaterColumn(240, 240, 0));
		}
	}

	public void update() {
		for (int i = 0; i < columns.size(); i++) {
			columns.get(i).update(dampening, tension);
		}

		float[] lDeltas = new float[columns.size()];
		float[] rDeltas = new float[columns.size()];

		// do some passes where columns pull on their neighbours
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < columns.size(); i++) {
				if (i > 0) {
					lDeltas[i] = this.spread * (columns.get(i).getHeight() - columns.get(i - 1).getHeight());
					columns.get(i - 1).setSpeed(columns.get(i - 1).getSpeed() + lDeltas[i]);
				}
				if (i < columns.size() - 1) {
					rDeltas[i] = this.spread * (columns.get(i).getHeight() - columns.get(i + 1).getHeight());
					columns.get(i + 1).setSpeed(columns.get(i + 1).getSpeed() + rDeltas[i]);
				}
			}

			for (int i = 0; i < columns.size(); i++) {
				if (i > 0)
					columns.get(i - 1).setHeight(columns.get(i - 1).getHeight() + lDeltas[i]);
				if (i < columns.size() - 1)
					columns.get(i + 1).setHeight(columns.get(i + 1).getHeight() + rDeltas[i]);
			}
		}

		List<Particle> res = new ArrayList<Particle>(particles);
		for (Particle particle : particles) {
			particle.update();
			if (predicate.evaluate(particle))
				res.add(particle);
		}

		particles = res;
	}

	public float getScale() {
		return Gdx.graphics.getWidth() / (columns.size() - 1f);
	}

	public float getHeight(float x) { // 800 ************************
		if (x < 0 || x > 800) {
			return 240;
		}
		return columns.get((int) (x / this.getScale())).getHeight();
	}

	public void splash(float xPosition, float speed) {
		int index = (int) this.clamp((int) (xPosition / this.getScale()), 0, columns.size() - 1);

		for (int i = Math.max(0, index); i < Math.min(columns.size() - 1, index + 1); i++)
			columns.get(index).setSpeed(speed);

		this.createSplashParticles(xPosition, speed);
	}

	private void createSplashParticles(float xPosition, float speed) {
		float y = this.getHeight(xPosition);

		if (speed > 120) {
			for (int i = 0; i < speed / 8; i++) {
				Vector2 pos = new Vector2(xPosition, y).add(this.getRandomVector(40));
				Vector2 vel = this.fromPolar((float) Math.toRadians(this.getRandomFloat(-150, -30)),
						this.getRandomFloat(0, 0.5f * (float) Math.sqrt(speed)));
				this.createParticle(pos, vel);
			}
		}
	}

	private void createParticle(Vector2 pos, Vector2 velocity) {
		particles.add(new Particle(pos, velocity, 0));
	}

	private Vector2 getRandomVector(float maxLength) {
		return this.fromPolar(this.getRandomFloat(-Math.PI, Math.PI), this.getRandomFloat(0, maxLength));
	}

	private float getRandomFloat(double min, double max) {
		return (float) (rand.nextDouble() * (max - min) + min);
	}

	private Vector2 fromPolar(float angle, float magnitude) {
		Vector2 res = new Vector2((float) Math.cos(angle), (float) Math.sin(angle));
		res.x = res.x * magnitude;
		res.y = res.y * magnitude;

		return res;
	}

	private int clamp(int num, int min, int max) {
		return num <= min ? min : num >= max ? max : num;
	}

	public List<WaterColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<WaterColumn> columns) {
		this.columns = columns;
	}

	public float getTension() {
		return tension;
	}

	public float getDampening() {
		return dampening;
	}

	public float getSpread() {
		return spread;
	}

}
