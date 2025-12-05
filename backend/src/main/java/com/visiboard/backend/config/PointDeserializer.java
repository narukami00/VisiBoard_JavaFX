package com.visiboard.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import java.io.IOException;

public class PointDeserializer extends JsonDeserializer<Point> {
    private final static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public Point deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null) return null;
        
        JsonNode coordinates = node.get("coordinates");
        if (coordinates != null && coordinates.isArray() && coordinates.size() >= 2) {
            double x = coordinates.get(0).asDouble();
            double y = coordinates.get(1).asDouble();
            return geometryFactory.createPoint(new Coordinate(x, y));
        }
        return null;
    }
}
