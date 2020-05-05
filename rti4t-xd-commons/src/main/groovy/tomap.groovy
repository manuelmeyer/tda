import org.codehaus.jackson.map.ObjectMapper

ObjectMapper mapper = new ObjectMapper();
return mapper.readValue(payload, HashMap.class);