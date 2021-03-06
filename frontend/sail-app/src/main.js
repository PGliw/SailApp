import Vue from "vue";
import App from "./App.vue";
import router from "./router";

import BootstrapVue from "bootstrap-vue";
import "bootstrap/dist/css/bootstrap.css";
import "bootstrap-vue/dist/bootstrap-vue.css";

var VueScrollTo = require('vue-scrollto');

Vue.use(BootstrapVue);
Vue.use(VueScrollTo);


Vue.config.productionTip = false;

export const eventBus = new Vue();

import * as VueGoogleMaps from 'vue2-google-maps';
import json from './json/secret.json';
Vue.use(VueGoogleMaps, {
  load: {
    key: json.API_KEY,
    libraries: "places"
  }
});

import axios from 'axios';
import VueAxios from 'vue-axios';
Vue.use(VueAxios, axios);


import { library } from '@fortawesome/fontawesome-svg-core';
import { faPhone } from '@fortawesome/free-solid-svg-icons';
import { faMapMarkerAlt } from '@fortawesome/free-solid-svg-icons';
import { faRoad } from '@fortawesome/free-solid-svg-icons';
import { faEnvelope } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
library.add(faPhone);
library.add(faMapMarkerAlt);
library.add(faRoad);
library.add(faEnvelope);
Vue.component('font-awesome-icon', FontAwesomeIcon);

new Vue({
  router,
  render: h => h(App)
}).$mount("#app");
