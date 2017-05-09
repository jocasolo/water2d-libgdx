package com.dream.water.effect;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;

import javafx.util.Pair;

public class MyContactListener implements ContactListener {

	@Override
	public void beginContact(Contact contact) {
		 Fixture fixtureA = contact.getFixtureA();
		 Fixture fixtureB = contact.getFixtureB();
		 
		 if(fixtureA.getBody().getUserData() instanceof Water && fixtureB.getBody().getType() == BodyType.DynamicBody){
			 Water water = (Water) fixtureA.getBody().getUserData();
			 water.getFixturePairs().add(new Pair<Fixture, Fixture>(fixtureA, fixtureB));
		 }
		 else if(fixtureB.getBody().getUserData() instanceof Water && fixtureA.getBody().getType() == BodyType.DynamicBody){
			 Water water = (Water) fixtureB.getBody().getUserData();
			 water.getFixturePairs().add(new Pair<Fixture, Fixture>(fixtureB, fixtureA));
		 }
	}

	@Override
	public void endContact(Contact contact) {
		Fixture fixtureA = contact.getFixtureA();
		Fixture fixtureB = contact.getFixtureB();
		 
		if(fixtureA.getBody().getUserData() instanceof Water && fixtureB.getBody().getType() == BodyType.DynamicBody){
			Water water = (Water) fixtureA.getBody().getUserData();
			 water.getFixturePairs().remove(new Pair<Fixture, Fixture>(fixtureA, fixtureB));
		}
		else if(fixtureB.getBody().getUserData() instanceof Water && fixtureA.getBody().getType() == BodyType.DynamicBody){
			Water water = (Water) fixtureB.getBody().getUserData();
			 water.getFixturePairs().add(new Pair<Fixture, Fixture>(fixtureA, fixtureB));
		}
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		// TODO Auto-generated method stub
		
	}

}
