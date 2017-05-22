package com.dream.water.effect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;

import javafx.util.Pair;

/**
 * Allows to create an object to simulate the behavior of water in interaction with other bodies
 */
public class Water implements Disposable {

	private boolean waves;
	private boolean splashParticles;
	private boolean debugMode;

	SpriteBatch spriteBatch;
	PolygonSpriteBatch polyBatch;
	ShapeRenderer shapeBatch;
	TextureRegion textureWater;
	Texture textureDrop;
	
	private Set<Pair<Fixture, Fixture>> fixturePairs; // contacts between this object and other dynamic bodies
	private List<WaterColumn> columns; // represent the height of the waves
	List<Particle> particles; // splash particles
	private Body body; // Box2d body

	private static Random rand = new Random();

	private float tension = 0.025f;
	private float dampening = 0.025f;
	private float spread = 0.25f;
	private float density = 0.85f;
	
	private final float columnSparation = 0.04f; // 4 px between every column
	private final float rotateCorrection = 0.0627f; // Manual correction to prevent rotation of bodies in contact with water

	/**
	 * Main constructor. Will create an object with the effect of waves and particles by default.
	 */
	public Water() {
		this(true, true);
	}

	/**
	 * Constructor that allows to specify if there is an effect of waves and splash particles.
	 * @param waves Specifies whether the object will have waves
	 * @param splashParticles Specifies whether the object will have splash particles
	 */
	public Water(boolean waves, boolean splashParticles) {

		this.waves = waves;
		this.splashParticles = splashParticles;
		this.fixturePairs = new HashSet<Pair<Fixture, Fixture>>();
		this.setDebugMode(false);

		if (waves) {
			textureWater = new TextureRegion(new Texture(Gdx.files.internal("water.png")));
			polyBatch = new PolygonSpriteBatch();
		}

		if (splashParticles) {
			textureDrop = new Texture(Gdx.files.internal("drop.png"));
			spriteBatch = new SpriteBatch();
			particles = new ArrayList<Particle>();
		}

		shapeBatch = new ShapeRenderer();
		shapeBatch.setColor(0, 0.5f, 1, 1);
	}

	/**
	 * Creates the body of the water. It will be a square sensor in a specific box2d world.
	 * @param world Our box2d world
	 * @param x Position of the x coordinate of the center of the body
	 * @param y Position of the y coordinate of the center of the body
	 * @param width Body width
	 * @param height Body height
	 * @param density Body density
	 */
	public void createBody(World world, float x, float y, float width, float height) {
		
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.set(x, y);

		// Create our body in the world using our body definition
		body = world.createBody(bodyDef);
		body.setUserData(this);
		
		PolygonShape square = new PolygonShape();
		square.setAsBox(width / 2, height / 2);

		// Create a fixture definition to apply our shape to
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = square;
		
		// Must be a sensor
		fixtureDef.isSensor = true;

		// Create our fixture and attach it to the body
		body.createFixture(fixtureDef);
		
		square.dispose();

		// Water columns (waves)
		if (waves) {
			int size = (int) (width / this.columnSparation);
			columns = new ArrayList<WaterColumn>(size);
			for (int i = 0; i < size+1; i++) {
				float cx = i * this.columnSparation + x - width / 2;
				columns.add(new WaterColumn(cx, y - height / 2, y + height / 2, y + height / 2, 0));
			}
		}
	}

	/**
	 * Updates the position of bodies in contact with water. To do this, it applies a force that counteracts
	 * gravity by calculating the area in contact, centroid and force required. 
	 */
	public void update() {
		
		if (body != null && fixturePairs != null) {
			World world = body.getWorld();
			for (Pair<Fixture, Fixture> pair : fixturePairs) {
				Fixture fixtureA = pair.getKey(); // water
				Fixture fixtureB = pair.getValue(); //dynamic body

				List<Vector2> intersectionPoints = new ArrayList<Vector2>();
				if (IntersectionUtils.findIntersectionOfFixtures(fixtureA, fixtureB, intersectionPoints)) {

					List<Vector2> actualIntersections = new ArrayList<Vector2>();
					actualIntersections = IntersectionUtils.copyList(intersectionPoints);

					// find centroid and area
					Polygon interPolygon = IntersectionUtils.getIntersectionPolygon(intersectionPoints);
					Vector2 centroid = new Vector2();
					GeometryUtils.polygonCentroid(interPolygon.getVertices(), 0, interPolygon.getVertices().length, centroid);
					float area = interPolygon.area();

					// apply buoyancy force (fixtureA is the fluid)
					float displacedMass = this.density * area;
					Vector2 buoyancyForce = new Vector2(displacedMass * -world.getGravity().x,
							displacedMass * -world.getGravity().y);
					fixtureB.getBody().applyForce(buoyancyForce, centroid, true);

					float dragMod = 0.25f; // adjust as desired
					float liftMod = 0.25f; // adjust as desired
					float maxDrag = 2000; // adjust as desired
					float maxLift = 500; // adjust as desired
					for (int i = 0; i < intersectionPoints.size(); i++) {
						Vector2 v0 = intersectionPoints.get(i);
						Vector2 v1 = intersectionPoints.get((i + 1) % intersectionPoints.size());
						Vector2 sum = v0.add(v1);
						Vector2 midPoint = new Vector2(0.5f * sum.x, 0.5f * sum.y);

						// find relative velocity between object and fluid at
						// edge midpoint
						Vector2 velDir = fixtureB.getBody().getLinearVelocityFromWorldPoint(midPoint)
								.sub(fixtureA.getBody().getLinearVelocityFromWorldPoint(midPoint));
						float vel = velDir.nor().len();

						Vector2 edge = v1.sub(v0);
						float edgeLength = edge.nor().len();
						Vector2 normal = new Vector2(-(-1) * edge.y, -1 * edge.x);
						float dragDot = normal.x * velDir.x + normal.y * velDir.y;
						if (dragDot < 0)
							continue;// normal points backwards - this is not a
										// leading edge

						// apply drag
						float dragMag = dragDot * dragMod * edgeLength * this.density * vel * vel;
						dragMag = Float.min(dragMag, maxDrag);
						Vector2 dragForce = new Vector2(dragMag * -velDir.x, dragMag * -velDir.y);
						fixtureB.getBody().applyForce(dragForce, midPoint, true);

						// apply lift
						float liftDot = edge.x * velDir.x + edge.y * velDir.y;
						float liftMag = dragDot * liftDot * liftMod * edgeLength * this.density * vel * vel;
						liftMag = Float.min(liftMag, maxLift);
						Vector2 liftDir = new Vector2(-1 * velDir.y, 1 * velDir.x);
						Vector2 liftForce = new Vector2(liftMag * liftDir.x, liftMag * liftDir.y);
						fixtureB.getBody().applyForce(liftForce, midPoint, true);

						// manual rotate correction
						float angularDrag = area * -fixtureB.getBody().getAngularVelocity() + rotateCorrection;
						fixtureB.getBody().applyTorque(angularDrag, true);
					}

					if (waves && area > 0.3f) {
						updateColumns(fixtureB.getBody(), actualIntersections);
					}
				}
			}
		}

		if (waves && splashParticles && !particles.isEmpty()) {
			updateParticles();
		}
	}

	/**
	 * Update the position of each particle
	 */
	private void updateParticles() {
		List<Particle> particlesCopy = new ArrayList<Particle>(particles);
		for (Particle particle : particles) {

			float elapsedTime = particle.getTime() + Gdx.graphics.getDeltaTime();

			float y = (float) (columns.get(0).getTargetHeight() + (Math.abs(particle.getVelocity().y) * elapsedTime)
					+ 0.5 * -10 * elapsedTime * elapsedTime);

			if (y < columns.get(0).getTargetHeight())
				particlesCopy.remove(particle);
			else {
				float x = (float) (particle.getInitX() + particle.getVelocity().x * elapsedTime);

				particle.setTime(elapsedTime);
				particle.setPosition(new Vector2(x, y));
			}
		}

		particles = particlesCopy;
	}

	/**
	 * Update the speed of each column in case that a body has touched it.
	 * @param body Body to evaluate
	 * @param intersectionPoints Part of the body that is in contact with water
	 */
	private void updateColumns(Body body, List<Vector2> intersectionPoints) {

		float minX = Float.MAX_VALUE;
		float maxX = Float.MIN_VALUE;

		for (Vector2 point : intersectionPoints) {
			minX = Float.min(minX, point.x);
			maxX = Float.max(maxX, point.x);
		}

		for (int i = 0; i < columns.size(); i++) {
			WaterColumn column = columns.get(i);

			if (column.x() >= minX && column.x() <= maxX) {
				// column points
				Vector2 col1 = new Vector2(column.x(), column.getHeight());
				Vector2 col2 = new Vector2(column.x(), body.getPosition().y - column.getHeight());

				for (int j = 0; j < intersectionPoints.size() - 1; j++) {
					// polygon, 1 line points
					Vector2 p1 = new Vector2(intersectionPoints.get(j).x, intersectionPoints.get(j).y);
					Vector2 p2 = null;
					if (j != intersectionPoints.size() - 1) {
						p2 = new Vector2(intersectionPoints.get(j + 1).x, intersectionPoints.get(j + 1).y);
					}

					Vector2 intersection = IntersectionUtils.intersection(col1, col2, p1, p2);
					if (intersection != null && intersection.y < column.getHeight()) {
						// column.setHeight((float) intersection.y());
						if (body.getLinearVelocity().y < 0 && column.getActualBody() == null) {
							column.setActualBody(body);
							column.setSpeed(body.getLinearVelocity().y * 3 / 100);
							if (splashParticles)
								this.createSplashParticles(column);
						}
					}
				}
			} else if (body == column.getActualBody()) {
				column.setActualBody(null);
			}

			if (body.getPosition().y < column.y() || column.getActualBody() != null && column.getActualBody().getPosition().y < column.y())
				column.setActualBody(null);
		}
	}

	/**
	 * Update the position of each column with respect to the speed that has been applied
	 */
	private void updateWaves() {
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

	}

	/**
	 * Create a new splash particle in the given position with a specific velocity
	 * @param pos Init position of the splash particle
	 * @param velocity Init velocity of the splash particle
	 */
	private void createParticle(Vector2 pos, Vector2 velocity, float radius) {
		Particle particle = new Particle(pos, velocity, radius);
		particle.setInitX(pos.x);
		particle.setTime(0);
		particle.setRadius(radius);
		particles.add(particle);
	}

	/**
	 * Creates particles in random position and velocity near to the body
	 * @param column We use it to know the speed of the body that is touching it
	 */
	private void createSplashParticles(WaterColumn column) {
		float y = column.getHeight();
		float bodyVel = Math.abs(column.getActualBody().getLinearVelocity().y);

		if (Math.abs(bodyVel) > 3f) {
			for (int i = 0; i < bodyVel / 8; i++) {
				Vector2 pos = new Vector2(column.x(), y).add(IntersectionUtils.getRandomVector(column.getTargetHeight()));

				Vector2 vel = new Vector2();
				if (rand.nextInt(4) == 0)
					vel = new Vector2(0, bodyVel / 2 + rand.nextFloat() * bodyVel / 2);
				else if (pos.x < column.getActualBody().getPosition().x)
					vel = new Vector2(-bodyVel / 5 + rand.nextFloat() * bodyVel / 5,
							bodyVel / 3 + rand.nextFloat() * bodyVel / 3);
				else
					vel = new Vector2(bodyVel / 5 + rand.nextFloat() * bodyVel / 5,
							bodyVel / 3 + rand.nextFloat() * bodyVel / 3);
				
				float radius = new Random().nextFloat() * (0.05f - 0.025f) + 0.025f;

				this.createParticle(pos, vel, radius);
			}
		}
	}

	/**
	 * Draws the waves and splash particles if they exist
	 * @param camera Camera used in the current stage
	 */
	public void draw(Camera camera) {

		if (hasWaves()) {
			
			updateWaves();

			polyBatch.setProjectionMatrix(camera.combined);
			shapeBatch.setProjectionMatrix(camera.combined);

			// draw columns water
			polyBatch.begin();
			for (int i = 0; i < columns.size() - 1; i++) {
				
				WaterColumn c1 = columns.get(i);
				WaterColumn c2 = columns.get(i + 1);
				
				if(!debugMode){
					float[] vertices = new float[] { c1.x(), c1.y(), c1.x(), c1.getHeight(), c2.x(), c2.getHeight(), c2.x(),
							c2.y() };
					PolygonSprite sprite = new PolygonSprite(new PolygonRegion(textureWater, vertices,
							new EarClippingTriangulator().computeTriangles(vertices).toArray()));
					sprite.draw(polyBatch, Math.min(1, Math.max(0.95f, c1.getHeight() / c1.getTargetHeight()))); // remove transparency for waves here if you don't want it
				}
				else {
					shapeBatch.begin(ShapeType.Line);
					shapeBatch.line(new Vector2(c1.x(), c1.y()), new Vector2(c1.x(), c1.getHeight()));
					shapeBatch.end();
				}
			}
			polyBatch.end();
			
			// draw splash particles
			if (hasSplashParticles()) {
				if(!debugMode){
					spriteBatch.setProjectionMatrix(camera.combined);
					spriteBatch.begin();
					for (Particle p : particles) {
						spriteBatch.draw(textureDrop, p.getPosition().x, p.getPosition().y, p.getRadius()*2, p.getRadius()*2);
					}
					spriteBatch.end();
				}
				else {
					shapeBatch.setProjectionMatrix(camera.combined);
					shapeBatch.begin(ShapeType.Line);
					for (Particle p : particles) {
						shapeBatch.rect(p.getPosition().x, p.getPosition().y, p.getRadius()*2, p.getRadius()*2);
					}
					shapeBatch.end();
				}
			}
			
		}
	}

	@Override
	public void dispose() {
		if(spriteBatch != null) spriteBatch.dispose();
		if(polyBatch != null) polyBatch.dispose();
		if(shapeBatch != null) shapeBatch.dispose(); 
		if(textureDrop != null) textureDrop.dispose();
		if(textureDrop != null) textureWater.getTexture().dispose();
		if(columns != null) columns.clear();
		if(particles != null) particles.clear();
		if(fixturePairs != null) fixturePairs.clear();
		if(body != null) body.getWorld().destroyBody(body);
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

	public boolean hasSplashParticles() {
		return splashParticles;
	}

	public List<Particle> getParticles() {
		return particles;
	}

	public void setParticles(List<Particle> particles) {
		this.particles = particles;
	}

	public Set<Pair<Fixture, Fixture>> getFixturePairs() {
		return fixturePairs;
	}

	public void setFixturePairs(Set<Pair<Fixture, Fixture>> fixturePairs) {
		this.fixturePairs = fixturePairs;
	}
	
	public float getDensity() {
		return density;
	}

	public void setDensity(float density) {
		this.density = density;
	}
	
	public float getColumnSparation() {
		return columnSparation;
	}

	public float getRotateCorrection() {
		return rotateCorrection;
	}

	public void setTension(float tension) {
		this.tension = tension;
	}

	public void setDampening(float dampening) {
		this.dampening = dampening;
	}

	public void setSpread(float spread) {
		this.spread = spread;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

}
