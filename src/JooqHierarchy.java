import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.*;
import org.jooq.impl.DSL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.util.*;
import static org.jooq.impl.DSL.*;

public class JooqHierarchy {
    record Content(Long id, String nodeId, List<Content> children) {};
    public static void main(String[] args) {

        String url = ConfigLoader.get("db.url");
        String user = ConfigLoader.get("db.user");
        String password = ConfigLoader.get("db.password");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DSLContext dsl = DSL.using(conn);
            Table<?> contents = table("ehr.composition_contents");


            Field<Long> compIdF = DSL.field("composition_id", Long.class);
            Field<String> compTypeF = DSL.field("composition_type", String.class);
            Field<String> archetypeIdF = DSL.field("archetype_id", String.class);
            Field<String> nodeIdF = DSL.field("node_id", String.class);
            Field<Integer> memberIndexF = DSL.field("member_index", Integer.class);
            Field<Long> parentIdF = DSL.field("parent_id", Long.class);
            Field<OffsetDateTime> timeF = DSL.field("time", OffsetDateTime.class);

            Field<Long> idF = DSL.field("id", Long.class);

            List<Content> hierarchy =
                    dsl.select(idF, parentIdF, nodeIdF, memberIndexF)
                            .from(contents)
                            .orderBy(idF)
                            .collect(Records.intoHierarchy(
                                    r -> r.value1(),
                                    r -> r.value2(),
                                    r -> new Content(r.value1(), r.value3(), new ArrayList<>()),
                                    (p, c) -> p.children().add(c)//p.children().put(c.nodeId(), c)
                            ));

            System.out.println(hierarchy);

            Map<String, Object> jsonStructure =  convertToJsonStructure("root", hierarchy);

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonStructure);
            System.out.println(json);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Map<String, Object> convertToJsonStructure(String parentString, List<Content> contents) {
        Map<String, Object> result = new LinkedHashMap<>();

        if(contents.isEmpty()) {
            result.put(parentString, null);
        }

        Map <String, Integer> cCnt = new HashMap<>();
        for (Content content : contents) {
            Integer memIndex = null;
            if(cCnt.containsKey(content.nodeId())) {
                memIndex = cCnt.get(content.nodeId());
            }
            String newKey = parentString + "/" + content.nodeId();
            if(memIndex != null) {
                newKey = newKey + ":" + memIndex;
            }
            result.putAll(convertToJsonStructure(newKey, content.children()));
            cCnt.put(content.nodeId(), cCnt.getOrDefault(content.nodeId(), 0) + 1);
        }
        return result;
    }
}
