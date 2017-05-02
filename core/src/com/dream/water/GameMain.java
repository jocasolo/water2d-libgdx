package com.dream.water;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.dream.water.effect.MyContactListener;
import com.dream.water.effect.Water;

public class GameMain extends ApplicationAdapter {

	Stage stage;
	OrthographicCamera camera;

	World world;
	Water water;
	MyContactListener contacts;
	Box2DDebugRenderer debugRenderer;

	@Override
	public void create() {
		camera = new OrthographicCamera();
		stage = new Stage(new StretchViewport(Gdx.graphics.getWidth() / 100, Gdx.graphics.getHeight() / 100, camera));

		// Create box2d world
		world = new World(new Vector2(0, -10), true);
		contacts = new MyContactListener();
		world.setContactListener(contacts);
		debugRenderer = new Box2DDebugRenderer();
		
		water = new Water();
		water.createBody(world, 0, 0, 8, 1, 1); //x, y, width, height, density
		water.setContactListener(contacts);
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
		
		water.update();
		world.step(1/60f, 6, 2);
		debugRenderer.render(world, camera.combined);
		
	}

	private void createBody() {
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		
		// Set our body's starting position in the world
		bodyDef.position.set(Gdx.input.getX() / 100f, camera.viewportHeight - Gdx.input.getY() / 100f);
		
		// Create our body in the world using our body definition
		Body body = world.createBody(bodyDef);

		// Create a circle shape and set its radius to 6
		PolygonShape square = new PolygonShape();
		square.setAsBox(1f, 1f);

		// Create a fixture definition to apply our shape to
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = square;
		fixtureDef.density = 0.5f;
		fixtureDef.friction = 0.5f;
		fixtureDef.restitution = 0.5f;

		// Create our fixture and attach it to the body
		body.createFixture(fixtureDef);

		square.dispose();
	}

	@Override
	public void dispose() {
		world.dispose();
		debugRenderer.dispose();
	}
}
