package projekt.model.buildings;

import javafx.beans.property.Property;
import org.tudalgo.algoutils.student.annotation.StudentImplementationRequired;
import projekt.model.HexGrid;
import projekt.model.Intersection;
import projekt.model.Player;
import projekt.model.TilePosition;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link Edge}.
 *
 * @param grid      the HexGrid instance this edge is placed in
 * @param position1 the first position
 * @param position2 the second position
 * @param roadOwner the road's owner, if a road has been built on this edge
 * @param port      a port this edge provides access to, if any
 */
public record EdgeImpl(
    HexGrid grid, TilePosition position1, TilePosition position2, Property<Player> roadOwner, Port port
) implements Edge {
    @Override
    public HexGrid getHexGrid() {
        return grid;
    }

    @Override
    public TilePosition getPosition1() {
        return position1;
    }

    @Override
    public TilePosition getPosition2() {
        return position2;
    }

    @Override
    public boolean hasPort() {
        return port != null;
    }

    @Override
    public Port getPort() {
        return port;
    }

    @Override
    @StudentImplementationRequired("H1.3")
    public boolean connectsTo(final Edge other) {
        // TODO H1.3 check
        return getIntersections().stream().anyMatch(a->{return other.getIntersections().stream().anyMatch(b->b.equals(a));});
    }

    @Override
    @StudentImplementationRequired("H1.3")
    public Set<Intersection> getIntersections() {
        Set<Intersection> result;
        // TODO: H1.3 better alternative solution is to get the overlapping Intersections of both Tiles(chose one way).
        /*
        //funktioniert nicht weil es den fall gibt wo eine der TilePos auserhalb des Spielfelds ist und deshalb keine Tile hat.
        Set<Intersection> otherSet = grid.getTileAt(position2).getIntersections();
        result = grid.getTileAt(position1).getIntersections().stream().filter(a->{return otherSet.contains(a);}).collect(Collectors.toSet());
        */



        //The harder way and correct way(because of edge of board case)

        //prework
        int qDiff = position1.q() - position2.q();
        int rDiff = position1.r() - position2.r();
        int sDiff = position1.s() - position2.s();

        TilePosition adj1 = null;
        TilePosition adj2 = null;
        //determines the axis of the edge and adjacent tile
        if(qDiff == 0){
            adj1 = new TilePosition(position2.q()+rDiff,position2.r());
            adj2 = new TilePosition(position2.q()+sDiff,position2.r()+rDiff);
        } else if (rDiff == 0) {
            adj1 = new TilePosition(position2.q(),position2.r()+qDiff);
            adj2 = new TilePosition(position2.q()+qDiff,position2.r()+sDiff);
        } else if (sDiff == 0) {
            adj1 = new TilePosition(position2.q(),position2.r()+rDiff);
            adj2 = new TilePosition(position2.q()+qDiff,position2.r());
        }
        //
        result= new HashSet<>();
        result.add(grid.getIntersectionAt(position1,position2,adj1));
        result.add(grid.getIntersectionAt(position1,position2,adj2));

        return result;

    }

    @Override
    public Property<Player> getRoadOwnerProperty() {
        return roadOwner;
    }

    @Override
    @StudentImplementationRequired("H1.3")
    public Set<Edge> getConnectedRoads(final Player player) {
        // TODO: H1.3 check
        return getConnectedEdges().stream().filter(a->{return (a.hasRoad())?  a.getRoadOwner().equals(player) : false;}).collect(Collectors.toSet());
    }

}
