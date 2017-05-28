# Water2D
Water simulation for libGDX games.

Allows to add a water type object to a Box2D world. This object, will interact with others dynamic bodies in the world in a similar way to  water.

It's based on the following tutorials:
- [Box2D C++ tutorials: Buoyancy](http://www.iforce2d.net/b2dtut/buoyancy) - by [iforce2d.net](http://www.iforce2d.net)
- [Make a Splash With Dynamic 2D Water Effects](https://gamedevelopment.tutsplus.com/tutorials/make-a-splash-with-dynamic-2d-water-effects--gamedev-236) - by [Michael Hoffman](https://tutsplus.com/authors/michael-hoffman)

## Final result
<img src="water.gif" alt="Image missing" width="400"/>

## Features
- Buoyancy
- Waves simulation
- Splash particles
- Interaction with others dynamic Box2D bodies
- Debug rendering

## Configuration
> It will be assumed that you already have a **Box2D world** and you just want to add a body that acts like water. If not, I recommend to you follow this tutorial: [Box2D](https://github.com/libgdx/libgdx/wiki/Box2d).
1. Add the following classes and assets to your project:
    - **Water:** the core class of the project.
    - **WaterColumn:** for waves simulation.
    - **Particle:** for splash particles
    - **IntersectionUtils:** utils for intersections, centroid, area, etc.
    - **water.png**
    - **drop.png**
   
2. Create a class that implements **ContactListener** interface and override the methods **beginContact()** and **endContact()** or modify the one you already have as below:
```java
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
```

3. Set your ContactListener class to your Box2D world:
```java
world.setContactListener(new MyContactListener());
```

4. Create a water object and its body position and size:
```java
water = new Water();
water.createBody(world, x, y, width, height);
```
> There is another water constructor **Water(boolean waves, boolean splashParticles)**, in case you don't want to render waves or splash particles.

5. And last but not least, in the **rendering part**, you should update and draw the water object. This needs an orthographic camera:
```java
water.update();
water.draw(camera);
```
You can see how everything is done in the classes **GameMain** and **MyContactListener**.

## Compile and execute
From the project root folder:
```
gradlew desktop:dist
java -jar desktop\build\libs\desktop-1.0.jar
```
