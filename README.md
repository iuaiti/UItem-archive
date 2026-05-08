# uitem - Custom Item and GUI Crafting Plugin

A Minecraft plugin for Paper 1.21 that adds custom items with GUIs for crafting, browsing, and administration.

## Features

- **Custom Items**: Define custom items with unique names, lore, custom model data, rarity, and glow effects.
- **Category System**: Organize items into categories for easy browsing.
- **GUI System**:
  - Main Menu: Browse categories
  - Category GUI: View items in a category with pagination (54 slots per page)
  - Crafting GUI: 3x3 crafting grid with real-time recipe validation
  - Admin GUI: Manage items and categories
- **Recipe System**: Create custom recipes for your items (shaped and shapeless).
- **Admin Tools**: Add, edit, remove items and categories, and edit recipes.
- **Persistence**: All data saved to YAML configuration files.
- **Adventure API**: Uses the Adventure API for modern text component support.
- **Performance Optimized**: Efficient data structures and minimal overhead.

## Installation

1. Download the latest `uitem.jar` from the [Releases](https://github.com/yourusername/uitem/releases) page.
2. Place the `.jar` file in your server's `plugins` folder.
3. Start or restart your server.
4. The plugin will generate default configuration files in `plugins/uitem/`.

## Usage

### Commands

- `/crafting` - Opens the crafting GUI.
- `/admin` - Opens the admin GUI (requires `uitem.admin` permission).

### Permissions

- `uitem.admin` - Allows access to the admin GUI and admin commands.

### GUIs

#### Main Menu
- Click on a category icon to browse items in that category.
- Click the "Crafting" button to open the crafting GUI.
- Click the "Admin" button to open the admin GUI (if you have permission).

#### Category GUI
- Displays items in the selected category with pagination.
- Use the "Previous Page" and "Next Page" buttons to navigate.
- Click on an item to view it (currently just a placeholder for future details).

#### Crafting GUI
- Place items in the 3x3 grid to craft.
- The result slot shows the crafted item if a valid recipe is matched.
- Click the result slot to take the crafted item (ingredients are consumed).

#### Admin GUI
- **Add Item**: Opens the Add Item GUI to create a new custom item.
- **Edit Item**: Opens the Edit Item GUI to modify an existing item.
- **Remove Item**: Opens the Remove Item GUI to delete an item.
- **Recipe Editor**: Opens the Recipe Editor GUI to set a recipe for an item.

## Configuration

The plugin creates the following files in `plugins/uitem/`:

- `config.yml` - Main configuration (currently not used for much, but can be extended).
- `items.yml` - Stores all custom items.
- `categories.yml` - Stores all categories.

### Example `items.yml`

```yaml
items:
  123e4567-e89b-12d3-a456-426614174000:
    id: 123e4567-e89b-12d3-a456-426614174000
    material: DIAMOND_SWORD
    name: '&eExcalibur'
    lore:
      - '&7A legendary sword'
      - '&7Only the worthy can wield it'
    custom_model_data: 123
    rarity: LEGENDARY
    glow: true
```

### Example `categories.yml`

```yaml
categories:
  weapons:
    name: weapons
    display_name: '&eWeapons'
    icon_item: DIAMOND_SWORD
    items:
      - 123e4567-e89b-12d3-a456-426614174000
  armor:
    name: armor
    display_name: '&fArmor'
    icon_item: DIAMOND_HELMET
    items: []
```

## Development

### Building

To build the plugin, you need:
- JDK 17
- Gradle 8.x

Run the following command to build the JAR:

```bash
gradle clean jar
```

The compiled JAR will be in `build/libs/`.

### Dependencies

- Paper API 1.21.0
- Adventure API 4.13.0

## Contributing

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add some amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

Your Name - [your.email@example.com](mailto:your.email@example.com)

Project Link: [https://github.com/yourusername/uitem](https://github.com/yourusername/uitem)