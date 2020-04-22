/**
 * Parse the landing page from config.js and add the result to the content container.
 */
function render_landing_page() {
  const content = $('#content');
  content.addClass('p-5');
  const landing_page_template = Handlebars.compile(
    $("#landing-page-template").html()
  );
  content.append(landing_page_template(_CONFIG));

  const sections = ["description", "awards", "development", "developers", "dependencies"];
  sections.forEach(function (section) {
    $.get("./config/home_page/" + section + ".html", function (data) {
      $("#" + section).append(data);
    });
  });
}


/**
 * Fetch and parse the tool info page.
 * @param tool_id
 */
function render_tool_page(tool_id) {
  const content = $('#content');
  content.addClass('p-5');
  $.get("./config/tool_pages/" + tool_id + ".html", function (data) {
    content.append(data);
  });
}


/**
 * Fetches the backend "ultimate" version and displays it in the settings menu.
 */
function load_backend_version() {
    $.get(_CONFIG.backend.web_bridge_url, "version", function (response) {
        try {
            $('#version_info_text').html(
                "Using ultimate version: " +
                response.ultimate_version
            );
        } catch (e) {
            console.log("Could not read backend ultimate version.");
            console.log(e);
        }
    });
}

/**
 * Load the interactive tool interface.
 * @param tool_id
 */
function load_tool_interface(tool_id) {
  load_tool_interface_template();
  init_editor();
  init_interface_controls();
  refresh_navbar();
  load_backend_version();
  set_message_orientation(_CONFIG.context.msg_orientation);
  if (_CONFIG.context.url.lang !== null) {
    choose_language(_CONFIG.context.url.lang);
    refresh_navbar();
  }
  if (_CONFIG.context.url.sample !== null) {
    load_sample(_CONFIG.context.url.sample);
  }
  if (_CONFIG.context.url.session !== null) {
    load_user_provided_session(_CONFIG.context.url.session);
  }
}


/**
 * Render the header/navigation-bar.
 */
function render_navbar() {
  const navbar_template = Handlebars.compile($("#navbar-template").html());
  $('#navbar_content').append(navbar_template(_CONFIG));
}


/**
 * Inject current context to _CONFIG.context s.t:
 *
 * _CONFIG.context = {
 *     url: {
 *         ui: <URL ui param | home by default.>
 *         tool: <URL tool param>
 *     },
 *     tool: <CONFIG for tool with corresponding tool.id>,
 *     msg_orientation: _CONFIG.editor.default_msg_orientation
 * }
 */
function set_context() {
  const url_params = get_url_params();
  let tool = {};

  // Load session if provided.
  if (url_params.session !== null){
    try {
      url_params.session = URIDecompressArray(url_params.session);
      url_params.tool = url_params.session.tool;
      url_params.ui = 'int';
    } catch (e) {
      alert('could not load Session provided. Malformed Link.');
      console.log(e);
    }
  }

  // Redirect non existing tools to home page.
  if (!tool_config_key_value_exists("id", url_params.tool)) {
    url_params.ui = "home";
  }

  // Set current tool if active.
  if (url_params.ui !== "home") {
    tool = Object.values(_CONFIG.tools).find(function (tool) {
      return tool.id === url_params.tool
    });
  }

  _CONFIG["context"] = {
    "url": url_params,
    "tool": tool,
    "msg_orientation": _CONFIG.editor.default_msg_orientation,
    "sample_source": ''
  }
}


function load_available_code_examples() {
  return $.getJSON("./config/code_examples/code_examples.json");
}


/**
 * Parse URL parameters and load/initialize corresponding content.
 */
function bootstrap() {
  set_context();
  render_navbar();

  // Load content by context.
  switch (_CONFIG.context.url.ui) {
    case "int":
      // load the interactive mode for the active tool.
      load_available_code_examples().always(function (json) {
        _CONFIG.code_examples = json;
        load_tool_interface(_CONFIG.context.tool.id);
      });
      break;
    case "tool":
      // load the tool info page.
      render_tool_page(_CONFIG.context.tool.id);
      break;
    default:
      // load the landing page.
      render_landing_page();
  }
}


$(function () {
  bootstrap();
});
