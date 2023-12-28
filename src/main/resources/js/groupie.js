(function ($) {
    var configUrl = AJS.contextPath() + "/rest/groupie/1.0/";
    var overviewUrl = AJS.contextPath() + "/rest/groupie/1.0/overview";

    // wait for the DOM
    $(function() {
        reload();

        function reload() {
            // request the config information from the server
            $.ajax({
                url: configUrl,
                dataType: "json"
            }).done(function(content) {
                // populate the form
                $("#mapping").val(JSON.stringify(content, null, 2));
            });

            // request the labels overview information from the server
            $.ajax({
                url: overviewUrl,
                dataType: "json"
            }).done(function(content) {
                // populate the form
                $("#security-labels-overview").val(JSON.stringify(content.securityLabelsOverview, null, 2));
                $("#standard-labels-overview").val(JSON.stringify(content.standardLabelsOverview, null, 2));
            });
        }

        function updateConfig() {
            $.ajax({
                url: configUrl,
                type: "PUT",
                contentType: "application/json",
                data: $("#mapping").val(),
                processData: false
            }).done(function(){
                alert("✅ Changes were successfully saved!");
                reload();
            }).fail(function(jqXHR, textStatus){
                alert(`❌ Invalid JSON, changes were not applied. (${textStatus}: ${jqXHR.status})`);
            });
        }

        $("#admin").on("submit", function(e) {
            e.preventDefault();
            updateConfig();
        });
    });
})(AJS.$ || jQuery);