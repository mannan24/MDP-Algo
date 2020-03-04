package map;

import robot.Robot;
import robot.RobotConstants;

import javax.swing.*;
import java.awt.*;

/**
 * Represents the entire map grid for the arena.
 *
 * @author Suyash Lakhotia
 */

public class Map extends JPanel {
    private final Cell[][] grid;
    private final Robot bot;

    /**
     * Initialises a Map object with a grid of Cell objects.
     */
    public Map(Robot bot) {
        this.bot = bot;

        grid = new Cell[MapConstants.MAP_ROWS][MapConstants.MAP_COLS];
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[0].length; col++) {
                grid[row][col] = new Cell(row, col);

                // Set the virtual walls of the arena
                if (row == 0 || col == 0 || row == MapConstants.MAP_ROWS - 1 || col == MapConstants.MAP_COLS - 1) {
                    grid[row][col].setVirtualWall(true);
                }
            }
        }
    }

    /**
     * Returns true if the row and column values are valid.
     */
    public boolean checkValidCoordinates(int row, int col) {
        return row >= 0 && col >= 0 && row < MapConstants.MAP_ROWS && col < MapConstants.MAP_COLS;
    }

    /**
     * Returns true if the row and column values are in the start zone.
     */
    private boolean inStartZone(int row, int col) {
        return row >= 0 && row <= 2 && col >= 0 && col <= 2;
    }

    /**
     * Returns true if the row and column values are in the goal zone.
     */
    private boolean inGoalZone(int row, int col) {
        return (row <= MapConstants.GOAL_ROW + 1 && row >= MapConstants.GOAL_ROW - 1 && col <= MapConstants.GOAL_COL + 1 && col >= MapConstants.GOAL_COL - 1);
    }

    /**
     * Returns a particular cell in the grid.
     */
    public Cell getCell(int row, int col) {
        return grid[row][col];
    }

    /**
     * Returns true if a cell is an obstacle.
     */
    public boolean isObstacleCell(int row, int col) {
        return grid[row][col].getIsObstacle();
    }

    /**
     * Returns true if a cell is a virtual wall.
     */
    public boolean isVirtualWallCell(int row, int col) {
        return grid[row][col].getIsVirtualWall();
    }

    /**
     * Sets all cells in the grid to an explored state.
     */
    public void setAllExplored() {
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[0].length; col++) {
                grid[row][col].setIsExplored(true);
            }
        }
    }

    /**
     * Sets all cells in the grid to an unexplored state except for the START & GOAL zone.
     */
    public void setAllUnexplored() {
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[0].length; col++) {
                if (inStartZone(row, col) || inGoalZone(row, col)) {
                    grid[row][col].setIsExplored(true);
                } else {
                    grid[row][col].setIsExplored(false);
                }
            }
        }
    }

    /**
     * Sets a cell as an obstacle and the surrounding cells as virtual walls or resets the cell and surrounding
     * virtual walls.
     */
    public void setObstacleCell(int row, int col, boolean obstacle) {
        if (obstacle && (inStartZone(row, col) || inGoalZone(row, col)))
            return;

        grid[row][col].setIsObstacle(obstacle);

        if (row >= 1) {
            grid[row - 1][col].setVirtualWall(obstacle);            // bottom cell

            if (col < MapConstants.MAP_COLS - 1) {
                grid[row - 1][col + 1].setVirtualWall(obstacle);    // bottom-right cell
            }

            if (col >= 1) {
                grid[row - 1][col - 1].setVirtualWall(obstacle);    // bottom-left cell
            }
        }

        if (row < MapConstants.MAP_ROWS - 1) {
            grid[row + 1][col].setVirtualWall(obstacle);            // top cell

            if (col < MapConstants.MAP_COLS - 1) {
                grid[row + 1][col + 1].setVirtualWall(obstacle);    // top-right cell
            }

            if (col >= 1) {
                grid[row + 1][col - 1].setVirtualWall(obstacle);    // top-left cell
            }
        }

        if (col >= 1) {
            grid[row][col - 1].setVirtualWall(obstacle);            // left cell
        }

        if (col < MapConstants.MAP_COLS - 1) {
            grid[row][col + 1].setVirtualWall(obstacle);            // right cell
        }
    }

    /**
     * Returns true if the given cell is out of bounds or an obstacle.
     */
    public boolean getIsObstacleOrWall(int row, int col) {
        return !checkValidCoordinates(row, col) || getCell(row, col).getIsObstacle();
    }

    /**
     * Overrides JComponent's paintComponent() method. It creates a two-dimensional array of _DisplayCell objects
     * to store the current map state. Then, it paints square cells for the grid with the appropriate colors as
     * well as the robot on-screen.
     */
    public void paintComponent(Graphics g) {
        // Create a two-dimensional array of _DisplayCell objects for rendering.
        _DisplayCell[][] _mapCells = new _DisplayCell[MapConstants.MAP_ROWS][MapConstants.MAP_COLS];
        for (int mapRow = 0; mapRow < MapConstants.MAP_ROWS; mapRow++) {
            for (int mapCol = 0; mapCol < MapConstants.MAP_COLS; mapCol++) {
                _mapCells[mapRow][mapCol] = new _DisplayCell(mapCol * GraphicsConstants.CELL_SIZE, mapRow * GraphicsConstants.CELL_SIZE, GraphicsConstants.CELL_SIZE);
            }
        }

        // Paint the cells with the appropriate colors.
        for (int mapRow = 0; mapRow < MapConstants.MAP_ROWS; mapRow++) {
            for (int mapCol = 0; mapCol < MapConstants.MAP_COLS; mapCol++) {
                Color cellColor;

                if (inStartZone(mapRow, mapCol))
                    cellColor = GraphicsConstants.C_START;
                else if (inGoalZone(mapRow, mapCol))
                    cellColor = GraphicsConstants.C_GOAL;
                else {
                    if (!grid[mapRow][mapCol].getIsExplored())
                        cellColor = GraphicsConstants.C_UNEXPLORED;
                    else if (grid[mapRow][mapCol].getIsObstacle())
                        cellColor = GraphicsConstants.C_OBSTACLE;
                    else
                        cellColor = GraphicsConstants.C_FREE;
                }

                g.setColor(cellColor);
                g.fillRect(_mapCells[mapRow][mapCol].cellX + GraphicsConstants.MAP_X_OFFSET, _mapCells[mapRow][mapCol].cellY, _mapCells[mapRow][mapCol].cellSize, _mapCells[mapRow][mapCol].cellSize);

            }
        }

        // Paint the robot on-screen.
        g.setColor(GraphicsConstants.C_ROBOT);
        int r = bot.getRobotPosRow();
        int c = bot.getRobotPosCol();
        g.fillOval((c - 1) * GraphicsConstants.CELL_SIZE + GraphicsConstants.ROBOT_X_OFFSET + GraphicsConstants.MAP_X_OFFSET, GraphicsConstants.MAP_H - (r * GraphicsConstants.CELL_SIZE + GraphicsConstants.ROBOT_Y_OFFSET), GraphicsConstants.ROBOT_W, GraphicsConstants.ROBOT_H);

        // Paint the robot's direction indicator on-screen.
        g.setColor(GraphicsConstants.C_ROBOT_DIR);
        RobotConstants.DIRECTION d = bot.getRobotCurDir();
        switch (d) {
            case NORTH:
                g.fillOval(c * GraphicsConstants.CELL_SIZE + 10 + GraphicsConstants.MAP_X_OFFSET, GraphicsConstants.MAP_H - r * GraphicsConstants.CELL_SIZE - 15, GraphicsConstants.ROBOT_DIR_W, GraphicsConstants.ROBOT_DIR_H);
                break;
            case EAST:
                g.fillOval(c * GraphicsConstants.CELL_SIZE + 35 + GraphicsConstants.MAP_X_OFFSET, GraphicsConstants.MAP_H - r * GraphicsConstants.CELL_SIZE + 10, GraphicsConstants.ROBOT_DIR_W, GraphicsConstants.ROBOT_DIR_H);
                break;
            case SOUTH:
                g.fillOval(c * GraphicsConstants.CELL_SIZE + 10 + GraphicsConstants.MAP_X_OFFSET, GraphicsConstants.MAP_H - r * GraphicsConstants.CELL_SIZE + 35, GraphicsConstants.ROBOT_DIR_W, GraphicsConstants.ROBOT_DIR_H);
                break;
            case WEST:
                g.fillOval(c * GraphicsConstants.CELL_SIZE - 15 + GraphicsConstants.MAP_X_OFFSET, GraphicsConstants.MAP_H - r * GraphicsConstants.CELL_SIZE + 10, GraphicsConstants.ROBOT_DIR_W, GraphicsConstants.ROBOT_DIR_H);
                break;
        }
    }

    private class _DisplayCell {
        public final int cellX;
        public final int cellY;
        public final int cellSize;

        public _DisplayCell(int borderX, int borderY, int borderSize) {
            this.cellX = borderX + GraphicsConstants.CELL_LINE_WEIGHT;
            this.cellY = GraphicsConstants.MAP_H - (borderY - GraphicsConstants.CELL_LINE_WEIGHT);
            this.cellSize = borderSize - (GraphicsConstants.CELL_LINE_WEIGHT * 2);
        }
    }
}
