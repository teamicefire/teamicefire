package ee.icefire.prison.escape.rooms;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrisonTest {

    // change these to your name:
    private static final String MY_FIRST_NAME = "Vladimir";
    private static final String MY_LAST_NAME = "Morozov";
    private static final String MY_KEY_CARD_DATA = MY_FIRST_NAME + "," + MY_LAST_NAME;

    private static final String OTHER_PRISONER_CARD_DATA = "Siim,Valdaru";
    private static final String SUPERVISOR_CARD_DATA = "Epp-Maria,Kivimaa";

    private KeyCardParser keyCardParser = new KeyCardParser();

    private List<PrisonRoom> rooms;
    
    @Before
    public void setUp() {
        PrisonGenerator prisonGenerator = new PrisonGenerator();
        rooms = prisonGenerator.generateTestPrison(
            new Person(MY_FIRST_NAME, MY_LAST_NAME),
            new Person("Siim", "Valdaru")
        );
    }

    @Test
    public void shouldAllowMeInEveryRoom() {
        KeyCardParser keyCardParser = new KeyCardParser();
        Person prisoner = keyCardParser.read(MY_KEY_CARD_DATA);

        boolean canEnterAllRooms = rooms.stream().allMatch(r -> r.allowsEntrance(prisoner));
        assertTrue(canEnterAllRooms);
    }

    @Test
    public void shouldNotAffectOthers() {
        keyCardParser.read(MY_KEY_CARD_DATA); // may have side effects

        // other prisoner
        Person otherPrisoner = keyCardParser.read(OTHER_PRISONER_CARD_DATA);
        
        PrisonRoom otherCell = PrisonRoom.getCellFor(otherPrisoner).orElseThrow(RuntimeException::new);
        assertTrue(otherCell.allowsEntrance(otherPrisoner));

        long otherPrisonersRoomsCount = rooms.stream()
            .filter(r -> r.allowsEntrance(otherPrisoner))
            .count();
        assertEquals(1, otherPrisonersRoomsCount);

        // supervisor
        Person supervisor = keyCardParser.read(SUPERVISOR_CARD_DATA);
        boolean canEnterAllRooms = rooms.stream().allMatch(r -> r.allowsEntrance(supervisor));
        assertTrue(canEnterAllRooms);
    }

    // extra points
    @Test
    public void parserShouldReadMyName() {
        KeyCardParser keyCardParser = new KeyCardParser();
        Person person = keyCardParser.read(MY_KEY_CARD_DATA);
        assertEquals(MY_FIRST_NAME, person.getFirstName());
        assertEquals(MY_LAST_NAME, person.getLastName());
    }

    // extra points
    @Test
    public void shouldNotLogName() {
        PrisonRoom myCell = PrisonRoom.getCellFor(new Person(MY_FIRST_NAME, MY_LAST_NAME))
            .orElseThrow(RuntimeException::new);

        boolean isNameInLogs = rooms.stream()
            .filter(r -> !r.equals(myCell)) // can log my name for my cell
            .map(PrisonRoom::toString)
            .anyMatch(s -> s.contains(MY_FIRST_NAME) || s.contains(MY_LAST_NAME));

        assertFalse(isNameInLogs);
    }

}

/**
 * Generates a prison to be used for testing
 */
class PrisonGenerator {

    private Random rand = new Random(2000L);
    private Person supervisor = new Person("Epp-Maria", "Kivimaa");
    private int id = 0;
    private List<PrisonRoom> rooms = new ArrayList<>();

    List<PrisonRoom> generateTestPrison(Person... prisoners) {
        PrisonRoom room = createRoom();

        Map<Person, PrisonRoom> cells = new HashMap<>();
        for (Person prisoner : prisoners) {
            HashSet<Person> allowedPersons = new HashSet<>();
            allowedPersons.add(supervisor);
            allowedPersons.add(prisoner);

            PrisonRoom neighbour = rooms.get(rand.nextInt(rooms.size()));

            PrisonRoom cell = new PrisonRoom(id++, allowedPersons);
            cell.getNeighbours().add(neighbour);
            neighbour.getNeighbours().add(cell);
            rooms.add(cell);
            cells.put(prisoner, cell);
        }

        PrisonRoom.setCells(cells);
        return rooms;
    }


    private PrisonRoom createRoom() {
        HashSet<Person> allowedPersons = new HashSet<>();
        allowedPersons.add(supervisor);
        PrisonRoom room = new PrisonRoom(id++, allowedPersons);
        rooms.add(room);

        int numberOfConnectedRooms = rand.nextInt(4);
        for (int i = 0; i < numberOfConnectedRooms; i++) {
            boolean isConnectedToNew = rand.nextBoolean();
            PrisonRoom neighbour;
            if (isConnectedToNew) {
                neighbour = createRoom();
            } else {
                neighbour = rooms.get(rand.nextInt(rooms.size()));
            }
            room.getNeighbours().add(neighbour);
            neighbour.getNeighbours().add(room);
        }

        return room;
    }

}
