/*
Stix2viz and d3 are packaged in a way that makes them work as Jupyter
notebook extensions.  Part of the extension installation process involves
copying them to a different location, where they're available via a special
"nbextensions" path.  This path is hard-coded into their "require" module
IDs.  Perhaps it's better to use abstract names, and add special config
in all cases to map the IDs to real paths, thus keeping the modules free
of usage-specific hard-codings.  But packaging in a way I know works in
Jupyter (an already complicated environment), and having only this config
here, seemed simpler.  At least, for now.  Maybe later someone can structure
these modules and apps in a better way.
*/
require.config({
    paths: {
      "nbextensions/stix2viz/d3": "stix2viz/d3/d3"
    }
});

require(["domReady!", "stix2viz/stix2viz/stix2viz"], function (document, stix2viz) {

    // Init some stuff
    // MATT: For optimization purposes, look into moving these to local variables
    selectedContainer = document.getElementById('selection');
    canvasContainer = document.getElementById('canvas-container');
    canvas = document.getElementById('canvas');
    cfg = { iconDir: "stix2viz/stix2viz/icons" };

    /* ******************************************************
     * Resizes the canvas based on the size of the window
     * ******************************************************/
    function resizeCanvas() {
      var cWidth = document.getElementById('legend').offsetLeft - 80;
      var cHeight = window.innerHeight - 100;
      document.getElementById('canvas-wrapper').style.width = cWidth;
      canvas.style.width = cWidth;
      canvas.style.height = cHeight;
    }

    /* ******************************************************
     * Will be called right before the graph is built.
     * ******************************************************/
    function vizCallback() {
      resizeCanvas();
    }

    /* ******************************************************
     * Initializes the graph, then renders it.
     * ******************************************************/
    function vizStixWrapper(content) {
      stix2viz.vizInit(canvas, cfg, populateLegend, populateSelected);
      stix2viz.vizStix(content, vizCallback);
    }

    /* ******************************************************
     * Handles content pasted to the text area.
     * ******************************************************/
    function handleTextarea() {
      content = document.getElementById('bundle-data').value;
      vizStixWrapper(content);
    }

     /* *****************************************************
      * Returns the page to its original load state
      * *****************************************************/
    function resetPage() {
        stix2viz.vizReset();
        document.getElementById('legend-content').innerHTML = ""; // reset the legend in the sidebar
        document.getElementById('canvas').innerHTML = "";
        document.getElementById('selection').innerHTML = "";
        document.getElementById('canvas-wrapper').innerHTML = "";
    }

    /* ******************************************************
     * Adds icons and information to the legend.
     *
     * Takes an array of type names as input
     * ******************************************************/
    function populateLegend(typeGroups) {
      var ul = document.getElementById('legend-content');
      typeGroups.forEach(function(typeName) {
        var li = document.createElement('li');
        var val = document.createElement('p');
        var key = document.createElement('div');
        key.style.backgroundImage = "url('stix2viz/stix2viz/icons/stix2_" + typeName.replace(/\-/g, '_') + "_icon_tiny_round_v1.png')";
        val.innerText = typeName.charAt(0).toUpperCase() + typeName.substr(1).toLowerCase(); // Capitalize it
        li.appendChild(key);
        li.appendChild(val);
        ul.appendChild(li);
      });
    }

    /* ******************************************************
     * Adds information to the selected node table.
     *
     * Takes datum as input
     * ******************************************************/
    function populateSelected(d) {
      // Remove old values from HTML
      selectedContainer.innerHTML = "";

      var counter = 0;

      Object.keys(d).forEach(function(key) { // Make new HTML elements and display them
        // Create new, empty HTML elements to be filled and injected
        var div = document.createElement('div');
        var type = document.createElement('div');
        var val = document.createElement('div');

        // Assign classes for proper styling
        if ((counter % 2) != 0) {
          div.classList.add("odd"); // every other row will have a grey background
        }
        type.classList.add("type");
        val.classList.add("value");

        // Add the text to the new inner html elements
        var value = d[key];
        type.innerText = key;
        val.innerText = value;

        // Add new divs to "Selected Node"
        div.appendChild(type);
        div.appendChild(val);
        selectedContainer.appendChild(div);

        // increment the class counter
        counter += 1;
      });
    }

    function selectedNodeClick() {
      selected = document.getElementById('selected');
      if (selected.className.indexOf('clicked') === -1) {
        selected.className += " clicked";
        selected.style.position = 'absolute';
        selected.style.left = '25px';
        selected.style.width = window.innerWidth - 110;
        selected.style.top = document.getElementById('legend').offsetHeight + 25;
        selected.scrollIntoView(true);
      } else {
        selected.className = "sidebar"
        selected.removeAttribute("style")
      }
    }

    /* ******************************************************
     * When the page is ready, setup the visualization and bind events
     * ******************************************************/
    window.onresize = resizeCanvas;
    handleTextarea();

  //  document.getElementById('bundle-data').addEventListener('click', handleTextarea, false);
  //  window.onresize = resizeCanvas;
});
