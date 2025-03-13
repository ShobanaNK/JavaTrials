import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

public class JooqHierarchy {
    record Content(Long id, String nodeId,
                    Double magnitude,
                    String unit,
                    Double numerator,
                   Double denominator,
                    String code,
                    String terminology,
                    String stringValue,
                   Boolean booleanValue,
                   List<Content> children) {};

    record ContentMap(Map<String, Object> resultMap,
                      List<ContentMap> children) {};

    static Table<?> contents = table("ehr.composition_contents");
    static Table<?> observations = table("ehr.composition_observation");


    static Field<Long> compIdF = DSL.field("composition_id", Long.class);
    static Field<String> compTypeF = DSL.field("composition_type", String.class);
    static Field<String> archetypeIdF = DSL.field("archetype_id", String.class);
    static Field<String> nodeIdF = DSL.field("node_id", String.class);
    static Field<Integer> memberIndexF = DSL.field("member_index", Integer.class);
    static Field<Long> parentIdF = DSL.field("parent_id", Long.class);
    static Field<OffsetDateTime> timeF = DSL.field("time", OffsetDateTime.class);

    static Field<Long> idCF = DSL.field("ehr.composition_contents.id", Long.class);

    static Field<Long> contentIdF = DSL.field("content_id", Long.class);
    static Field<Double> magnitudeF = DSL.field("magnitude", Double.class);
    static Field<String> unitF = DSL.field("unit", String.class);
    static Field<Double> numeratorF = DSL.field("numerator", Double.class);
    static Field<Double> denominatorF = DSL.field("denominator", Double.class);
    static Field<String> codedValueF = DSL.field("code", String.class);
    static Field<String> codedTerminologyF = DSL.field("terminology", String.class);
    static Field<String> stringValueF = DSL.field("string_value", String.class);
    static Field<Boolean> booleanValueF = DSL.field("boolean_value", Boolean.class);

    static List<String> nonObservationElements = Arrays.asList(idCF.getName(), parentIdF.getName(), nodeIdF.getName());


    public static void main(String[] args) {

        String url = ConfigLoader.get("db.url");
        String user = ConfigLoader.get("db.user");
        String password = ConfigLoader.get("db.password");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DSLContext dsl = DSL.using(conn);


            List<ContentMap> hierarchy =
                    dsl.select(idCF, parentIdF, nodeIdF, magnitudeF, unitF, numeratorF, denominatorF, codedValueF, codedTerminologyF, stringValueF, booleanValueF)
                            .from(contents)
                            .leftOuterJoin(observations)
                            .on(idCF.eq(contentIdF)) // Join condition
                            .orderBy(idCF)
                            .collect(Records.intoHierarchy(
                                    r -> r.value1(),
                                    r -> r.value2(),
                                    r -> new ContentMap(r.intoMap(), new ArrayList<>()),
                                    (p, c) -> p.children().add(c)//p.children().put(c.nodeId(), c)
                            ));

            System.out.println(hierarchy);

            Map<String, Object> jsonStructure =  convertToJsonStructure(null, hierarchy);

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonStructure);
            System.out.println(json);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Map<String, Object> convertToJsonStructure(String parentString,List<ContentMap> contents) {
        Map<String, Object> result = new LinkedHashMap<>();

        Map <String, Integer> cCnt = new HashMap<>();
        for (ContentMap content : contents) {
            Integer memIndex = null;
            String contentNodeId = (String) content.resultMap().get(nodeIdF.getName());
            System.out.println(contentNodeId);
            if(cCnt.containsKey(contentNodeId)) {
                memIndex = cCnt.get(contentNodeId);
            }
            String newKey = contentNodeId;
            if(memIndex != null) {
                newKey = newKey + ":" + memIndex;
            }
            if (parentString != null) {
                newKey = parentString + "/" + newKey;
            }


            if (content.children().isEmpty()) {
                final String keyPrefix = newKey;
                //result.put(newKey, null);
                Map<String, Object> observationMap = content.resultMap().entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() != null)
                        .filter(entry -> !nonObservationElements.contains(entry.getKey()) )
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                // Print the cleaned map
                System.out.println("Filtered Map: " + observationMap);

                observationMap.forEach( (key, value) -> {
                    if (key.matches("[a-z]+_value")) {
                        result.put(keyPrefix, value);
                    } else {
                        result.put(keyPrefix + '|' + key, value);
                    }
                });



            } else {
                result.putAll(convertToJsonStructure(newKey, content.children()));
            }

            cCnt.put(contentNodeId, cCnt.getOrDefault(contentNodeId, 0) + 1);
        }
        return result;
    }
}
