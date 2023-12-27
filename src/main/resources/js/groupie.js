(function ($) {
    var configUrl = AJS.contextPath() + "/rest/groupie/1.0/";
    var overviewUrl = AJS.contextPath() + "/rest/groupie/1.0/overview";

    // wait for the DOM
    $(function() {
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
            $("#filtered_overview").val(JSON.stringify(content.filteredSpacesAndLabels, null, 2));
            $("#unused_labels").val(JSON.stringify(content.availableSpacesAndLabels, null, 2));
        });

        function updateConfig() {
                    $.ajax({
                        url: configUrl,
                        type: "PUT",
                        contentType: "application/json",
                        data: $("#mapping").val(),
                        processData: false
                    }).done(function(){
                        alert("✅ Changes were successfully saved!");
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