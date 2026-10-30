# Ocean theme state coverage

`ocean.css` is loaded after the legacy component stylesheet. Popup list cells require selectors at least as specific as the original `.combo-box-popup > .list-view > .virtual-flow > .clipped-container > .sheet > .list-cell` rules. Default, hover, focus and selected states are explicitly covered; the path selector uses the same text colors as other ComboBoxes. Prompt labels nested inside TextInputDialog/ChoiceDialog grids are themed as well as direct DialogPane children.

Manage Folders uses class-based row/path styling instead of inline white backgrounds and dark text. Its list viewport is 280 pixels preferred, grows with the resizable dialog, and rows use compact padding. Empty feedback does not occupy layout space. Sidebar folder rows are 29 pixels high. Removing a shortcut updates saved roots and cached selection without deleting filesystem contents.

`VolumePicker` is an owned popup containing the existing full disk cards and a bounded scroll viewport. It toggles from the same owner button, handles outside clicks in the owner scene, Escape, card selection and owner geometry changes. The popup scene wrapper is transparent so legacy root styling does not produce a white surround. Existing storage-card data and selection handling are reused.

`OceanComponents.typeColor(FileType)` defines stable semantic colors: images mint, videos purple, audio pink, documents blue, archives amber, code cyan, and other gray. Pie slices, legend dots and progress bars share this mapping; slice order, amount and display language do not change a file type's color. JavaFX's existing sector shading is retained.

Regression tests cover popup states, workspace removal, manager sizing/colors, many-volume scrolling/toggle/selection, semantic colors with reversed slice order, and nested dialog prompt styling. A separate local UI audit covers all eight pages and populated popups in English and Simplified Chinese.
