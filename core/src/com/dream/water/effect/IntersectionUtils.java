package com.dream.water.effect;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.utils.Box2DBuild;

public class IntersectionUtils {

	public static boolean inside(Vector2 cp1, Vector2 cp2, Vector2 p) {
		return (cp2.x - cp1.x) * (p.y - cp1.y) > (cp2.y - cp1.y) * (p.x - cp1.x);
	}

	public static Vector2 intersection(Vector2 cp1, Vector2 cp2, Vector2 s, Vector2 e) {
		Vector2 dc = new Vector2(cp1.x - cp2.x, cp1.y - cp2.y);
		Vector2 dp = new Vector2(s.x - e.x, s.y - e.y);
		float n1 = cp1.x * cp2.y - cp1.y * cp2.x;
		float n2 = s.x * e.y - s.y * e.x;
		float n3 = 1.0f / (dc.x * dp.y - dc.y * dp.x);
		return new Vector2((n1 * dp.x - n2 * dc.x) * n3, (n1 * dp.y - n2 * dc.y) * n3);
	}

	public static boolean findIntersectionOfFixtures(Fixture fA, Fixture fB, List<Vector2> outputVertices) {
		// currently this only handles polygon vs polygon
		if (fA.getShape().getType() != Shape.Type.Polygon || fB.getShape().getType() != Shape.Type.Polygon)
			return false;

		PolygonShape polyA = (PolygonShape) fA.getShape();
		PolygonShape polyB = (PolygonShape) fB.getShape();

		// fill subject polygon from fixtureA polygon
		for (int i = 0; i < polyA.getVertexCount(); i++) {
			Vector2 vertex = new Vector2();
			polyA.getVertex(i, vertex);
			vertex = fA.getBody().getWorldPoint(vertex);
			outputVertices.add(new Vector2(vertex));
		}

		// fill clip polygon from fixtureB polygon
		List<Vector2> clipPolygon = new ArrayList<Vector2>();
		for (int i = 0; i < polyB.getVertexCount(); i++) {
			Vector2 vertex = new Vector2();
			polyB.getVertex(i, vertex);
			vertex = fB.getBody().getWorldPoint(vertex);
			clipPolygon.add(new Vector2(vertex));
		}

		Vector2 cp1 = clipPolygon.get(clipPolygon.size() - 1);
		for (int j = 0; j < clipPolygon.size(); j++) {
			Vector2 cp2 = clipPolygon.get(j);
			if (outputVertices.isEmpty())
				return false;
			List<Vector2> inputList = new ArrayList<Vector2>(outputVertices);
			outputVertices.clear();
			Vector2 s = inputList.get(inputList.size() - 1); // last on the
																// input list
			for (int i = 0; i < inputList.size(); i++) {
				Vector2 e = inputList.get(i);
				if (inside(cp1, cp2, e)) {
					if (!inside(cp1, cp2, s)) {
						outputVertices.add(intersection(cp1, cp2, s, e));
					}
					outputVertices.add(e);
				} else if (inside(cp1, cp2, s)) {
					outputVertices.add(intersection(cp1, cp2, s, e));
				}
				s = e;
			}
			cp1 = cp2;
		}

		return !outputVertices.isEmpty();
	}

	public static Vector2 getCentroid(List<Vector2> vertices) {
		Vector2 centroid = new Vector2(0, 0);

		for (Vector2 vertex : vertices) {
			centroid.x += vertex.x;
			centroid.y += vertex.y;
		}
		return new Vector2(centroid.x / vertices.size(), centroid.y / vertices.size());
	}

	public static float getArea(List<Vector2> vertices) {
		float sum = 0;

		for (int i = 0; i < vertices.size(); i++) {
			if (i == 0) {
				sum += vertices.get(i).x * (vertices.get(i + 1).y - vertices.get(vertices.size() - 1).y);
			} else if (i == vertices.size() - 1) {
				sum += vertices.get(i).x * (vertices.get(0).y - vertices.get(i - 1).y);
			} else {
				sum += vertices.get(i).x * (vertices.get(i + 1).y - vertices.get(i - 1).y);
			}
		}

		float area = 0.5f * Math.abs(sum);
		return area;
	}
	
	public static Vector2 min(Vector2 a, Vector2 b){
		return new Vector2(min(a.x, b.x), min(b.x, b.y));
	}
	
	public static float min(float a, float b){
		return a < b ? a : b;
	}
}
