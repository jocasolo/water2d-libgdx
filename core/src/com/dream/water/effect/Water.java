package com.dream.water.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Predicate;

import javafx.util.Pair;
import math.geom2d.Point2D;
import math.geom2d.line.Line2D;
import math.geom2d.line.StraightLine2D;
import math.geom2d.polygon.SimplePolygon2D;

public class Water {

	private boolean waves = false;
	private List<WaterColumn> columns; // for waves
	List<Particle> particles = new ArrayList<Particle>();
	private Body body; // Box2d body
	
	private MyContactListener contacts;

	private static Random rand = new Random();

	private final float tension = 0.025f;
	private final float dampening = 0.025f;
	private final float spread = 0.25f;
	private final float columnSparation = 0.04f;
	private final float rotateCorrection = 0.0627f;

	Predicate<Particle> predicate;
	
	public Water(boolean waves){
		this.setWaves(waves);
	}
	
	public void createBody(World world, float x, float y, float width, float height, float density) {
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.set(x, y);

		// Create our body in the world using our body definition
		body = world.createBody(bodyDef);

		PolygonShape square = new PolygonShape();
		square.setAsBox(width/2, height/2);

		// Create a fixture definition to apply our shape to
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = square;
		fixtureDef.density = density;
		
		// Must be a sensor
		fixtureDef.isSensor = true;

		// Create our fixture and attach it to the body
		body.createFixture(fixtureDef);

		square.dispose();
		
		// Water columns (waves) *******************************************
		if(waves){
			int size = (int) (width / this.columnSparation);
			columns = new ArrayList<WaterColumn>(size);
			for (int i = 0; i < size; i++) {
				float cx = i * this.columnSparation + x - width/2;
				columns.add(new WaterColumn(cx, y-height/2, y+height/2, y+height/2, 0));
			}
			
			final float w = width;
			predicate = new Predicate<Particle>() {
				@Override
				public boolean evaluate(Particle p) {
					return p.getPosition().x >= 0 && p.getPosition().x <= w
							&& p.getPosition().y - 5 <= getHeight(p.getPosition().x); 
				}
			};
		}
		// *********************************************************
	}
	
	public void setContactListener(MyContactListener contacts){
		this.contacts = contacts; 
	}
	
	public void update(){
		
		if(body != null && contacts != null){
			World world = body.getWorld();
			for(Pair<Fixture, Fixture> pair : contacts.getFixturePairs()){
				Fixture fixtureA = pair.getKey();
				Fixture fixtureB = pair.getValue();
				
				float density = fixtureA.getDensity();
				
				List<Vector2> intersectionPoints = new ArrayList<Vector2>();
				if(IntersectionUtils.findIntersectionOfFixtures(fixtureA, fixtureB, intersectionPoints)){
					
					if(waves){
						updateColumns(intersectionPoints);
					}
					
					//find centroid and area
					SimplePolygon2D interPolygon = IntersectionUtils.getIntersectionPolygon(intersectionPoints);
					Point2D centroidPoint = interPolygon.centroid();
					Vector2 centroid = new Vector2((float) centroidPoint.x(), (float) centroidPoint.y());
					float area = (float) interPolygon.area();
					
					//apply buoyancy force (fixtureA is the fluid)
					float displacedMass = fixtureA.getDensity() * area;
					Vector2 buoyancyForce = new Vector2(displacedMass * -world.getGravity().x, displacedMass * -world.getGravity().y);
					fixtureB.getBody().applyForce(buoyancyForce, centroid, true);
					
					float dragMod = 0.25f; 	//adjust as desired
	                float liftMod = 0.25f; 	//adjust as desired
	                float maxDrag = 2000;  	//adjust as desired
	                float maxLift = 500;   	//adjust as desired
	                for (int i = 0; i < intersectionPoints.size(); i++) {
	                    Vector2 v0 = intersectionPoints.get(i);
	                    Vector2 v1 = intersectionPoints.get((i+1)%intersectionPoints.size());
	                    Vector2 sum = v0.add(v1);
	                    Vector2 midPoint = new Vector2(0.5f * sum.x, 0.5f * sum.y);

	                    //find relative velocity between object and fluid at edge midpoint
	                    Vector2 velDir = fixtureB.getBody().getLinearVelocityFromWorldPoint( midPoint ).sub(fixtureA.getBody().getLinearVelocityFromWorldPoint(midPoint));
	                    float vel = velDir.nor().len();

	                    Vector2 edge = v1.sub(v0);
	                    float edgeLength = edge.nor().len();
	                    Vector2 normal = new Vector2(-(-1) * edge.y, -1 * edge.x);
	                    float dragDot = normal.x * velDir.x + normal.y * velDir.y;
	                    if ( dragDot < 0 )
	                        continue;//normal points backwards - this is not a leading edge

	                    //apply drag
	                    float dragMag = dragDot * dragMod * edgeLength * density * vel * vel;
	                    dragMag = IntersectionUtils.min(dragMag, maxDrag);
	                    Vector2 dragForce = new Vector2(dragMag * -velDir.x, dragMag * -velDir.y);
	                    fixtureB.getBody().applyForce(dragForce, midPoint, true);

	                    //apply lift
	                    float liftDot = edge.x * velDir.x + edge.y * velDir.y;
	                    float liftMag =  dragDot * liftDot * liftMod * edgeLength * density * vel * vel;
	                    liftMag = IntersectionUtils.min(liftMag, maxLift);
	                    Vector2 liftDir = new Vector2(-1 * velDir.y, 1 * velDir.x);
	                    Vector2 liftForce = new Vector2(liftMag * liftDir.x, liftMag * liftDir.y);
	                    fixtureB.getBody().applyForce(liftForce, midPoint, true);
	                    
	                    // rotate correction
	                    float angularDrag = area * -fixtureB.getBody().getAngularVelocity()+rotateCorrection;
	                    fixtureB.getBody().applyTorque( angularDrag , true);
	                }
					
				}
				
			}
		}
	}
	
	private void updateColumns(List<Vector2> intersectionPoints){
		
		List<Point2D> points = new ArrayList<Point2D>();
		for(Vector2 point : intersectionPoints){
			points.add(new Point2D(point.x, point.y));
		}
		
		for(int i = 0; i < columns.size(); i++){
			WaterColumn column = columns.get(i);
			if(column.x() >= 0){
				// column points
				Point2D col1 = new Point2D(column.x(), column.getHeight());
				Point2D col2 = new Point2D(column.x(), body.getPosition().y-column.getHeight());
				
				for(int j = 0; j<intersectionPoints.size(); j++){
					// polygon, 1 line points
					Point2D p1 = new Point2D(intersectionPoints.get(j).x, intersectionPoints.get(j).y);
					Point2D p2 = null;
					if(j != intersectionPoints.size()-1){
						p2 = new Point2D(intersectionPoints.get(j+1).x, intersectionPoints.get(j+1).y);
					} else {
						p2 = new Point2D(intersectionPoints.get(0).x, intersectionPoints.get(0).y);
					}
					
					// lines for polygon and column
					Line2D line1 = new Line2D(col1, col2);
					Line2D line2 = new Line2D(p1, p2);
					
					long elapsedTime = System.currentTimeMillis() - column.getStartTime();
					if((elapsedTime < 150 || column.getStartTime() == 0)  && Line2D.intersects(line1, line2)){
						if(column.getStartTime() == 0)
							column.setStartTime(System.currentTimeMillis());
						Point2D intersection = StraightLine2D.getIntersection(col1, col2, p1, p2);
						if(intersection != null && intersection.y() < column.getHeight())
							column.setHeight((float) intersection.y());
					}
				}
			}
			else {
				column.setStartTime(0);
			}
		}
	}

	public void updateWaves() {
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

	public boolean hasWaves() {
		return waves;
	}

	public void setWaves(boolean waves) {
		this.waves = waves;
	}

}
