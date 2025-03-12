import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jooq.*;
import org.jooq.impl.DSL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static org.jooq.impl.DSL.*;

public class JooqHierarchy {
    record Content(Long id, String nodeId, List<Content> children) {};
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5431/mydatabase";
        String user = "myuser";
        String password = "secret";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DSLContext dsl = DSL.using(conn);

            // Define tables and fields
            Table<?> contents = table("ehr.composition_contents");
            // Table<?> departments = table("departments");


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
            /*for (Content content : hierarchy) {
                jsonStructure.put(content.nodeId(), content);
            }
*/
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonStructure);
            /*objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print JSON

            String jsonOutput = objectMapper.writeValueAsString(jsonStructure);*/

            // Step 3: Print JSON
            System.out.println(json);

          /*  // Fetch the hierarchical data using intoHierarchy()
            Map<Record, List<Record>> hierarchy = dsl
                .select(deptName, empId, empName)
                .from(departments)
                .join(employees)
                .on(field("departments.dept_id").eq(field("employees.dept_id")))
                .fetch()
                .intoHierarchy(deptName);  // Use `dept_name` as the grouping key

            // Convert to desired JSON-like structure
            Map<String, List<Map<String, Object>>> result = hierarchy.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> entry.getKey().get(deptName), // Use dept_name as dynamic key
                    entry -> entry.getValue().stream()
                        .map(record -> Map.of(
                            "emp_id", record.get(empId),
                            "emp_name", record.get(empName)
                        ))
                        .collect(Collectors.toList())
                ));

            // Print the result
            System.out.println(result);*/


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*public static void updateChildren(Content child, Map<String, List<Content>> children) {
        String newKey = child.nodeId();
        if (!children.containsKey(newKey)) {
            children.put(newKey, new ArrayList<Content>());
        }
        children.get(newKey).add(child);
    }*/

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
            /*if (result.containsKey(newKey)) {
                //children.put(newKey, new ArrayList<Content>());

                if ( result.get(newKey) instanceof List<?>) {
                    ((List<Object>) result.get(newKey)).add(convertToJsonStructure(newKey, content.children()));
                } else {
                    Object oldContent = result.get(newKey);
                    ArrayList<Object> multiElement = new ArrayList<>();
                    multiElement.add(oldContent);
                    multiElement.add(convertToJsonStructure(newKey, content.children()));
                    result.put(newKey, multiElement);
                }

            } else {*/
            result.putAll(convertToJsonStructure(newKey, content.children()));
            cCnt.put(content.nodeId(), cCnt.getOrDefault(content.nodeId(), 0) + 1);
            //}
        }
        return result;
    }
}
