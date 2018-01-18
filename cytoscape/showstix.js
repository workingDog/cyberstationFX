
document.addEventListener('DOMContentLoaded',function(){

function showBundleGraph() {
// console.log("in showBundleGraph");
      content = document.getElementById('bundle-data').value;
      var cy = cytoscape({
        container: document.getElementById('cy'),
        elements: JSON.parse(content),
          style: [{ selector: 'node', style: {
//          	'background-image': function(el){
//          		if(el.data('image')){
//          		  	return el.data('image') +  ' /icons/stix2_indicator_icon_tiny_round_v1.png';
//          		}else{
//          		  	return ['/icons/stix2_attack_pattern_icon_tiny_round_v1.png'];
//          		}
            shape: 'roundrectangle',
            'width': 'label',
            'height': 'label',
            'border-width': 1,
            'border-color': 'black',
            'background-color': 'red',
            'background-opacity': 0.7,
            'text-background-color': 'white',
            'color': 'black', // colour of the element's label.
           // 'opacity': 1,
            'active-bg-color': 'white',
          //  'outside-texture-bg-color': 'white',
            'label': function(ele) {
                            var txt = ele.data('id').split("--")[0];
                            if(txt === undefined){
                                return "node"
                            } else {
                                return txt
                            }
                        }
          }},
          { selector: 'edge', style: {
                'width': 2,
                'line-color': 'blue'  // '#aaa'
          }}]
      });

cy.layout({
            name: 'cose', // 'cose-bilkent'
            animate: 'end',
            animationEasing: 'ease-out',
            animationDuration: 1000,
            randomize: true
}).run();

//cy.resize();

}

document.getElementById('bundle-data').addEventListener('click', showBundleGraph, false);

});
