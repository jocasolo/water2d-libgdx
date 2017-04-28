package com.dream.water.effect;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;

import javafx.util.Pair;

public class BodyContact implements ContactListener {

	private Set<Pair<Fixture, Fixture>> fixturePairs = new HashSet<Pair<Fixture, Fixture>>();
	
	@Override
	public void beginContact(Contact contact) {
		 Fixture fixtureA = contact.getFixtureA();
		 Fixture fixtureB = contact.getFixtureB();
		 
		 if(fixtureA.isSensor() && fixtureB.getBody().getType() == BodyType.DynamicBody)
			 fixturePairs.add(new Pair<Fixture, Fixture>(fixtureA, fixtureB));
		 else if(fixtureB.isSensor() && fixtureA.getBody().getType() == BodyType.DynamicBody)
			 fixturePairs.add(new Pair<Fixture, Fixture>(fixtureB, fixtureA));
	}

	@Override
	public void endContact(Contact contact) {
		Fixture fixtureA = contact.getFixtureA();
		Fixture fixtureB = contact.getFixtureB();
		 
		if(fixtureA.isSensor() && fixtureB.getBody().getType() == BodyType.DynamicBody)
			fixturePairs.remove(new Pair<Fixture, Fixture>(fixtureA, fixtureB));
		else if(fixtureB.isSensor() && fixtureA.getBody().getType() == BodyType.DynamicBody)
			fixturePairs.remove(new Pair<Fixture, Fixture>(fixtureB, fixtureA));
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		// TODO Auto-generated method stub
		
	}
	
	public Set<Pair<Fixture, Fixture>> getFixturePairs() {
		return fixturePairs;
	}

	public void setFixturePairs(Set<Pair<Fixture, Fixture>> fixturePairs) {
		this.fixturePairs = fixturePairs;
	}

}
