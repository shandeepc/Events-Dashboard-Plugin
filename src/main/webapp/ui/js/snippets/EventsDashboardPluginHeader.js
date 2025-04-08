var url =
  SailPoint.CONTEXT_PATH + "/plugins/pluginPage.jsf?pn=EventsDashboardPlugin";
jQuery(document).ready(function () {
  jQuery("ul.navbar-right li:first").before(
    '<li class="dropdown">' +
      '		<a href="' +
      url +
      '" tabindex="0" role="menuitem" title="Events Dashboard">' +
      '			<i role="presenation" class="fa fa-cubes fa-lg"></i>' +
      "		</a>" +
      "</li>"
  );
});
