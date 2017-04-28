package com.dream.water;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.dream.water.effect.BodyContact;
import com.dream.water.effect.IntersectionUtils;

import javafx.util.Pair;

public class GameMain extends ApplicationAdapter {
	SpriteBatch batch;

	Stage stage;
	OrthographicCamera camera;

	World world;
	BodyContact contacts;
	Box2DDebugRenderer debugRenderer;
	ShapeRenderer shapeRenderer;

	@Override
	public void create() {
		batch = new SpriteBatch();

		camera = new OrthographicCamera();
		stage = new Stage(new StretchViewport(4f, 2f, camera));
		shapeRenderer = new ShapeRenderer();
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.setColor(1, 1, 0, 1);

		// Create box2d world
		world = new World(new Vector2(0, -10), true);
		contacts = new BodyContact();
		world.setContactListener(contacts);
		debugRenderer = new Box2DDebugRenderer();

		createWaterBody();
	}

	@Override
	public void render () {
		// Clean screen
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		// Input
		if(Gdx.input.justTouched()){
			createBody();
		}

		world.step(1/60f, 6, 2);
		debugRenderer.render(world, camera.combined);
		
		//*********************
		shapeRenderer.begin(ShapeType.Line);
		for(Pair<Fixture, Fixture> pair : contacts.getFixturePairs()){
			Fixture fixtureA = pair.getKey();
			Fixture fixtureB = pair.getValue();
			
			float density = fixtureA.getDensity();
			
			List<Vector2> intersectionPoints = new ArrayList<Vector2>();
			if(IntersectionUtils.findIntersectionOfFixtures(fixtureA, fixtureB, intersectionPoints)){
				
				//add lines to draw the intersection in debug
				for(int i = 0; i<intersectionPoints.size(); i++){
					if(i != intersectionPoints.size()-1)
						shapeRenderer.line(intersectionPoints.get(i), intersectionPoints.get(i+1));
					else
						shapeRenderer.line(intersectionPoints.get(i), intersectionPoints.get(0));
				}
				
				//find centroid
				float area = IntersectionUtils.getArea(intersectionPoints);
				Vector2 centroid = IntersectionUtils.getCentroid(intersectionPoints);
				
				//apply buoyancy force (fixtureA is the fluid)
				float displacedMass = fixtureA.getDensity() * area;
				Vector2 buoyancyForce = new Vector2(displacedMass * -world.getGravity().x, displacedMass * -world.getGravity().y);
				fixtureB.getBody().applyForce(buoyancyForce, centroid, false);
				
				//find relative velocity between object and fluid
				/*Vector2 velDir = new Vector2(fixtureB.getBody().getLinearVelocityFromWorldPoint(centroid).sub(fixtureA.getBody().getLinearVelocityFromWorldPoint(centroid)));
		        float vel = velDir.nor().len();
		      
			    //apply simple linear drag
			    float dragMag = fixtureA.getDensity() * vel * vel;
			    Vector2 dragForce = new Vector2(dragMag * -velDir.x, dragMag * -velDir.y);
			    fixtureB.getBody().applyForce(dragForce, centroid, false);
			    
			    //apply simple angular drag
			    float angularDrag = area * -fixtureB.getBody().getAngularVelocity();
			    fixtureB.getBody().applyTorque(angularDrag, false);*/
				
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
                    fixtureB.getBody().applyForce(dragForce, midPoint, false);

                    //apply lift
                    float liftDot = edge.x * velDir.x + edge.y * velDir.y;
                    float liftMag =  dragDot * liftDot * liftMod * edgeLength * density * vel * vel;
                    liftMag = IntersectionUtils.min(liftMag, maxLift);
                    Vector2 liftDir = new Vector2(-1 * velDir.y, 1 * velDir.x);
                    Vector2 liftForce = new Vector2(liftMag * liftDir.x, liftMag * liftDir.y);
                    fixtureB.getBody().applyForce(liftForce, midPoint, false);
                }
				
                //line showing buoyancy force
                /*if(area > 0){
        			shapeRenderer.line(centroid.x, centroid.y, centroid.x, centroid.y + area);
        		}*/
			}
			
		}
		shapeRenderer.end();
		
	}

	private void createBody() {
		// First we create a body definition
		BodyDef bodyDef = new BodyDef();
		// We set our body to dynamic, for something like ground which doesn't
		// move we would set it to StaticBody
		bodyDef.type = BodyType.DynamicBody;
		// Set our body's starting position in the world
		Vector2 position = new Vector2(Gdx.input.getX() / 100f, camera.viewportHeight - Gdx.input.getY() / 100f);
		bodyDef.position.set(Gdx.input.getX() / 100f, camera.viewportHeight - Gdx.input.getY() / 100f);

		// Create our body in the world using our body definition
		Body body = world.createBody(bodyDef);

		// Create a circle shape and set its radius to 6
		PolygonShape square = new PolygonShape();
		//square.setAsBox(0.5f, 0.3f);
		Vector2[] vertices = new Vector2[5];
		vertices[0] = (new Vector2(1f, 1.2f));
		vertices[1]= (new Vector2(1.4f, 1.2f));
		vertices[2]= (new Vector2(1.4f, 0.8f));
		vertices[3]= (new Vector2(1.1f, 0f));
		vertices[4]= (new Vector2(1f, 0.8f));
		square.set((Vector2[]) vertices);

		// Create a fixture definition to apply our shape to
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = square;
		fixtureDef.density = 0.5f;
		fixtureDef.friction = 0.5f;
		fixtureDef.restitution = 0.5f; // Make it bounce a
															// little bit

		// Create our fixture and attach it to the body
		Fixture fixture = body.createFixture(fixtureDef);

		// Remember to dispose of any shapes after you're done with them!
		// BodyDef and FixtureDef don't need disposing, but shapes do.
		square.dispose();
	}

	private void createWaterBody() {
		// First we create a body definition
		BodyDef bodyDef = new BodyDef();
		// We set our body to dynamic, for something like ground which doesn't
		// move we would set it to StaticBody
		bodyDef.type = BodyType.StaticBody;
		// Set our body's starting position in the world
		bodyDef.position.set(0, 0);

		// Create our body in the world using our body definition
		Body body = world.createBody(bodyDef);

		// Create a circle shape and set its radius to 6
		PolygonShape square = new PolygonShape();
		square.setAsBox(camera.viewportWidth, camera.viewportHeight / 3);

		// Create a fixture definition to apply our shape to
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = square;
		fixtureDef.density = 1f;
		fixtureDef.friction = 0.5f;
		fixtureDef.restitution = 0.5f; // Make it bounce a
															// little bit
		fixtureDef.isSensor = true;

		// Create our fixture and attach it to the body
		Fixture fixture = body.createFixture(fixtureDef);

		// Remember to dispose of any shapes after you're done with them!
		// BodyDef and FixtureDef don't need disposing, but shapes do.
		square.dispose();
	}

	@Override
	public void dispose() {
		batch.dispose();
		world.dispose();
		debugRenderer.dispose();
	}
}
