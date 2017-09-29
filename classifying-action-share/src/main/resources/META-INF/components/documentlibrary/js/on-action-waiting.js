(function() {
 
    YAHOO.Bubbling.fire("registerAction", {
        actionName : "onActionWaiting",
        fn: function onActionWaitingSimpleRepoAction(record, owner) {
             
            this.widgets.waitDialog = Alfresco.util.PopupManager.displayMessage({
                text : this.msg("classifying-action.waiting"),
                spanClass : "wait",
                displayTime : 0
            });
             
            // Get action params
             var params = this.getAction(record, owner).params,
                displayName = record.displayName,
                namedParams = ["function", "action", "success", "successMessage", "failure", "failureMessage"],
                repoActionParams = {};
 
             for (var name in params)
             {
                if (params.hasOwnProperty(name) && !Alfresco.util.arrayContains(namedParams, name))
                {
                   repoActionParams[name] = params[name];
                }
             }
 
             // Deactivate action
             var ownerTitle = owner.title;
             owner.title = owner.title + "_deactivated";
 
             // Prepare genericAction config
             var config =
             {
                success:
                {
                   event:
                   {
                      name: "metadataRefresh",
                      obj: record
                   }
                },
                failure:
                {
                	event:
                    {
                       name: "metadataRefresh",
                       obj: record
                    }
                },
                webscript:
                {
                   method: Alfresco.util.Ajax.POST,
                   stem: Alfresco.constants.PROXY_URI + "api/",
                   name: "actionQueue"
                },
                config:
                {
                   requestContentType: Alfresco.util.Ajax.JSON,
                   dataObj:
                   {
                      actionedUponNode: record.nodeRef,
                      actionDefinitionName: params.action,
                      parameterValues: repoActionParams
                   }
                }
             };
 
             // Add configured success callbacks and messages if provided
             if (YAHOO.lang.isFunction(this[params.success]))
             {
                config.success.callback =
                {
                   fn: this[params.success],
                   obj: record,
                   scope: this
                };
             }
             if (params.successMessage)
             {
                config.success.message = this.msg(params.successMessage, displayName);
             }
 
             // Acd configured failure callback and message if provided
             if (YAHOO.lang.isFunction(this[params.failure]))
             {
                config.failure.callback =
                {
                   fn: this[params.failure],
                   obj: record,
                   scope: this
                };
             }
             if (params.failureMessage)
             {
                config.failure.message = this.msg(params.failureMessage, displayName);
             }
 
             // Execute the repo action
             this.modules.actions.genericAction(config);
             
          }     
    });         
     
})();