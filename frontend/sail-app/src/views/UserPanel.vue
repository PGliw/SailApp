<template>
  <b-container class="UserPanel">
    <div v-if="breachAlert == false">
      <div v-if="loading == false">
        <br>
        <b-tabs content-class="mt-3" align="center">
          <b-tab title="Rent" active>
            <br>
            <h1 class='title'>Rent page</h1>
            <br>
            <br>
            <h4>
              Sort by Distance:
              <b-button-group>
                <b-button variant="primary" v-on:click="sortNear()"> Nearest </b-button>
                <b-button variant="primary" v-on:click="sortFurth()"> Furthest </b-button>
              </b-button-group>
            </h4>
            <br>
            <div v-for="(companyForm, index) in companyForms" :key="index">
              <CompanyCard :parentUserForm=userForm :parentCompanyForm=companyForm :parentRent=rent @SendRentFormParent="FillRentForm" />
              <br>
            </div>
          </b-tab>
          <b-tab title="User Panel">
            <br>
            <h1 class='title'>User Panel</h1>
            <br>
            <h6>User: {{userForm.email}}</h6>
            <br>
            <br>
            <b-row>
              <b-col sm="9">
                <b-form-input :readonly='changeName' class="block" type="text" v-model='userForm.name' placeholder="First Name" />
                <b-form-input :readonly='changeSurname' class="block" type="text" v-model='userForm.surname' placeholder="Second Name" />
                <b-form-input :readonly='changeTel' class="block" type="tel" v-model='userForm.phone' placeholder="Phone number" />
                <b-form-input :readonly='changePassword' class="block" type="password" v-model='userForm.password' placeholder="Password" />
                <b-form-input :readonly='changePassword' class="block" type="password" v-model='userForm.checkPassword' placeholder="Repeat Password" />
              </b-col>
              <b-col sm="3">
                <b-button block class="block" variant="info" v-on:click="changeNameProp()">Change</b-button>
                <b-button block class="block" variant="info" v-on:click="changeSurnameProp()">Change</b-button>
                <b-button block class="block" variant="info" v-on:click="changeTelProp()">Change</b-button>
                <b-button block class="block" variant="info" v-on:click="changePasswordProp()">Change</b-button>
                <b-button block class="block" variant="info" v-on:click="changePasswordProp()">Change</b-button>
              </b-col>
            </b-row>
            <b-button block variant="success" v-on:click="Change()">Change</b-button>
            <b-button block variant="warning" to="/">Homepage</b-button>
          </b-tab>
          <b-tab title="Upcoming events">
            <br>
            <h1 class='title'>Upcoming events</h1>
            <br>
            <br>
            <b-container v-if="rent == true">
              <b-card title='Rent' :sub-title="'status: ' + rentForm.rent_status">
                <b-card-text>
                  Your Water Cenre: <b>{{rentForm.centre_name}}</b>
                  <br>
                  Phone number: <b>{{rentForm.centre_phone_number}}</b>
                  <br>
                  Rent Start: <b>{{rentForm.rent_start.toLocaleString()}}</b>
                  <br>
                  Type of Gear: <b>{{rentForm.gear_name}}</b>
                  <br>
                  Place: <b>{{rentForm.place}}</b>
                  <br>
                  Cost: <b>{{rentForm.cost}}</b>
                </b-card-text>
                <b-button variant="danger" v-on:click="CancelRent()">Cancel</b-button>
              </b-card>
            </b-container>
            <b-container v-if="rent == false">
              <br>
              <br>
              <h2 align="center">Rent something!</h2>
            </b-container>
          </b-tab>
        </b-tabs>
      </div>
      <div v-if="loading == true" class="text-center">
        <b-spinner style="width: 200px; height: 200px;" class="m-25" variant="primary" type="grow"></b-spinner>
      </div>
    </div>
    <div v-if="breachAlert == true || breachAlert == null">
      <h3>You have to be log in to view this site, go to the <b-link href="/">homepage</b-link>!</h3>
    </div>
  </b-container>
</template>

<script>
import CompanyCard from '@/components/CompanyCard.vue';
import apiKey from '../json/secret.json';

export default {
  name: "UserPanel",
  props: ['user'],
  components: {
    CompanyCard
  },
  data() {
    return {
      userForm: {
        role: '',
        name: '',
        surname: '',
        phone: '',
        email: '',
        password: '',
        checkPassword: ''
      },
      rentForm: {
        rent_start: '',
        rent_end: '',
        rent_amount: 1,
        is_returned: null,
        user_id: '',
        gear_id: '',
        gear_centre_id: '',
        place: null,
        cost: null,
        centre_name: null,
        gear_name: null,
        rent_id: null,
        rent_status: null,
        centre_phone_number: null
      },
      companyForms: [],
      changeName: true,
      changeSurname: true,
      changeTel: true,
      changePassword: true,
      changeCompanyName: true,
      changeCompanyTel: true,
      breachAlert: null,
      rent: null,
      loading: true
    }
  },
  methods: {
    changeNameProp(){
      this.changeName = !this.changeName;
    },
    changeSurnameProp(){
      this.changeSurname = !this.changeSurname;
    },
    changeTelProp(){
      this.changeTel = !this.changeTel;
    },
    changePasswordProp(){
      this.changePassword = !this.changePassword;
    },
    calcDistAll(){
      let obj = this;
      for (let i = 0; i < this.companyForms.length; i++) {
        if (navigator.geolocation) {
          navigator.geolocation.getCurrentPosition(function(position) {
            let currentLat = position.coords.latitude;
            let currentLng = position.coords.longitude;
            let R = 6371e3; // metres
            let φ1 = Number(obj.companyForms[i].latitude) * Math.PI / 180;
            let φ2 = currentLat * Math.PI / 180;
            let Δφ = (currentLat-Number(obj.companyForms[i].latitude)) * Math.PI / 180;
            let Δλ = (currentLng-Number(obj.companyForms[i].longtitude)) * Math.PI / 180;

            let a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                    Math.cos(φ1) * Math.cos(φ2) *
                    Math.sin(Δλ/2) * Math.sin(Δλ/2);
            let c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            
            let d = R * c;
            let result = Math.floor(d/1000);
            obj.companyForms[i].dist = result;
          });
        } else {
          //console.log('No geolocation error');
        }
      }
      this.loading = false;
    },
    sortNear(){
      this.companyForms.sort((a, b) => (a.dist > b.dist) ? 1 : -1);
    },
    sortFurth(){
      this.companyForms.sort((a, b) => (a.dist < b.dist) ? 1 : -1);
    },
    FillRentForm (value) {
      this.rentForm.rent_start = value.rent_start;
      this.rentForm.rent_end = value.rent_end;
      this.rentForm.rent_amount = value.rent_amount;
      this.rentForm.gear_centre_id = value.gear_centre_id;
      this.rentForm.gear_id = value.gear_id;


      var obj = this;
      let data = new FormData();
      data.append("centre_id", this.rentForm.gear_centre_id);
      data.append("gear_id", this.rentForm.gear_id);
      data.append("rent_amount", this.rentForm.rent_amount);
      data.append("rent_start", this.rentForm.rent_start.toISOString());
      data.append("rent_end", this.rentForm.rent_end.toISOString());
      this.axios
      .post("http://127.0.0.1:8000/projekt-gospodarka-backend.herokuapp.com/rental/rentGear", data, {
        headers: {
          'X-Requested-With': 'http://projekt-gospodarka-backend.herokuapp.com/rental/rentGear',
          'Content-Type': 'multipart/form-data',
          'accept': 'application/json',
          'Authorization': "Bearer " + this.user.token
        }
      })
      .then(
        (response) => {
          obj.getRentData();
        })
    },
    CancelRent(){
      var obj = this;
      let dataR = new FormData();
      dataR.append("rent_id", Number(this.rentForm.rent_id));
      this.axios
      .put("http://127.0.0.1:8000/projekt-gospodarka-backend.herokuapp.com/rental/cancelRent", dataR,{
        headers: {
          'X-Requested-With': 'http://projekt-gospodarka-backend.herokuapp.com/rental/cancelRent',
          'Content-Type': 'multipart/form-data',
          'accept': 'application/json',
          'Authorization': "Bearer " + this.user.token
        }
      })
      .then(
        (response) => {
          obj.getRentData();
          //console.log(JSON.stringify(response));
        });
    },
    Change(){
      //console.log("User " + JSON.stringify(this.form) + " changed");
      var obj = this;
      //User Data
      let dataU = new FormData();
      dataU.append("first_name", this.userForm.name);
      dataU.append("last_name", this.userForm.surname);
      dataU.append("email", this.userForm.email);
      dataU.append("phone_number", this.userForm.phone);
      this.axios
      .post("http://127.0.0.1:8000/projekt-gospodarka-backend.herokuapp.com/accounts/changeData", dataU, {
        headers: {
          'X-Requested-With': 'http://projekt-gospodarka-backend.herokuapp.com/accounts/changeData',
          'Content-Type': 'multipart/form-data',
          'accept': 'application/json',
          'Authorization': "Bearer " + this.user.token
        }
      });
      //Password
      let dataP = new FormData();
      dataP.append("password", this.userForm.password);
      this.axios
      .post("http://127.0.0.1:8000/projekt-gospodarka-backend.herokuapp.com/accounts/changePassword", dataP, {
        headers: {
          'X-Requested-With': 'http://projekt-gospodarka-backend.herokuapp.com/accounts/changePassword',
          'Content-Type': 'multipart/form-data',
          'accept': 'application/json',
          'Authorization': "Bearer " + this.user.token
        }
      });
    },
    calCost(){
      for(let i = 0; i < this.companyForms.length; i++){
        if(this.companyForms[i].name == 'Water Centre #1'){
          for(let j = 0; j < this.companyForms[i].gears.length; j++){
            if(this.companyForms[i].gears[j].gearType == 'Boast'){
              this.rentForm.cost = this.rentForm.rent_amount * this.companyForms[i].gears[j].gearCost;
            }
          }
        }
      }
    },
    calcPlace(lat, lng){
      let obj = this;
      this.axios
      .get("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&key=" + apiKey.API_KEY2)
      .then(
        (response) => {
          this.rentForm.place = response.data.results[0].address_components[3].long_name + ', '
                       + response.data.results[0].address_components[1].long_name + ', '
                       + response.data.results[0].address_components[0].long_name;
        })
      this.calCost();
    },
    getRentData(){
      var obj = this;
      this.axios
      .get("http://127.0.0.1:8000/projekt-gospodarka-backend.herokuapp.com/gear/getMyRentedGear", {
        headers: {
          'X-Requested-With': 'http://projekt-gospodarka-backend.herokuapp.com/gear/getMyRentedGear',
          'accept': 'application/json',
          'Authorization': "Bearer " + this.user.token
        }
      })
      .then(
        (response) => {
          //console.log(JSON.stringify(response));
          if(response.data.length == 0){
            obj.rent = false;
            //console.log('xd tutaj');
          }else{
            obj.rent = true;
            obj.rentForm.rent_start = new Date(response.data[0].rent_start);
            obj.rentForm.rent_end = new Date(response.data[0].rent_end);
            obj.rentForm.rent_amount = response.data[0].rent_quantity
            obj.rentForm.is_returned = false;
            obj.rentForm.gear_centre_id = response.data[0].centre_id;
            obj.rentForm.rent_id = response.data[0].rent_id;
            obj.rentForm.rent_status = response.data[0].rent_status;
            obj.rentForm.gear_name = response.data[0].gear_name;
            obj.rentForm.centre_name = response.data[0].centre_name;
            obj.rentForm.centre_phone_number = response.data[0].centre_phone_number;
            obj.calcPlace(response.data[0].centre_latitude, response.data[0].centre_longitude);
          }
        })
      .catch(function (error){
        console.log(error);
      });
    },
    getUserData(){
      //obsluzyc rent
      var obj = this;
      this.axios
      .get("http://127.0.0.1:8000/projekt-gospodarka-backend.herokuapp.com/accounts/getUserData", {
        headers: {
          'X-Requested-With': 'http://projekt-gospodarka-backend.herokuapp.com/accounts/getUserData',
          'accept': 'application/json',
          'Authorization': "Bearer " + this.user.token
        }
      })
      .then(
        (response) => {
          this.userForm.name = response.data.first_name;
          this.userForm.surname = response.data.last_name;
          this.userForm.phone = response.data.phone_number;
          obj.getRentData();
        })
      .catch(function (error){
        console.log(error);
      });
    },
    getAllCentreData(){
      this.getAllCentreBasicData();
    },
    getAllCentreBasicData(){
      var obj = this;
      this.axios
      .get("http://127.0.0.1:8000/projekt-gospodarka-backend.herokuapp.com/centre/getCentres", {
        headers: {
          'X-Requested-With': 'http://projekt-gospodarka-backend.herokuapp.com/centre/getCentres',
          'accept': 'application/json',
          'Authorization': "Bearer " + this.user.token
        }
      })
      .then(
        (response) => {
          for (let i = 0; i < response.data.length; i++) {
            obj.companyForms.push({
              name: response.data[i].centre_name,
              latitude: response.data[i].latitude,
              longtitude: response.data[i].longitude,
              phone: response.data[i].phone_number,
              centre_id: response.data[i].centre_id,
              gears: [],
              photo: null,
              dist: null
            });
          }
          obj.getAllCentrePicId();
        })
      .catch(function (error){
        console.log(error);
      });
    },
    getAllGear(){
      let obj = this;
      for (let i = 0; i < this.companyForms.length; i++) {
        this.axios
          .get("http://127.0.0.1:8000/projekt-gospodarka-backend.herokuapp.com/gear/getAllGear/" + this.companyForms[i].centre_id, {
            headers: {
              'X-Requested-With': 'http://projekt-gospodarka-backend.herokuapp.com/gear/getAllGear/',
              'Authorization': "Bearer " + this.user.token
            }
          })
          .then(
            (response) => {
              for (let j = 0; j < response.data.length; j++) {
                obj.companyForms[i].gears.push({
                  "id" : response.data[j].gear_id.toString(),
                  "gearType" : response.data[j].gear_name.toString(),
                  "gearAmount" : response.data[j].gear_quantity.toString(),
                  "gearCost" : response.data[j].gear_price.toString()
                });
              }
            })
      }
      obj.calcDistAll();
    },
    getAllCentrePicId(){
      let obj = this;
      for (let i = 0; i < this.companyForms.length; i++) {
        this.axios
          .get("http://127.0.0.1:8000/projekt-gospodarka-backend.herokuapp.com/user/getPicturesOfCentre/" + this.companyForms[i].centre_id, {
            headers: {
              'X-Requested-With': 'http://projekt-gospodarka-backend.herokuapp.com/user/getPicturesOfCentre/',
              'Authorization': "Bearer " + this.user.token
            }
          })
          .then(
            (response) => {
              obj.companyForms[i].photo = response.data[response.data.length - 1].picture_link;
            })
      }
      obj.getAllGear();
    }
  },
  created () {
    if(this.user.role == 'user'){
      this.userForm.role = this.user.role;
      this.userForm.email = this.user.login;
      this.userForm.password = this.user.password;
      this.userForm.checkPassword = this.user.password;

      this.getUserData();
      this.getAllCentreData();
      this.breachAlert = false;

    }else{
      this.breachAlert = true;
    }
  }
};
</script>

<style scope>
  .title {
    background: linear-gradient(180deg, rgba(255,255,255,0) 65%, #FFD0AE 65%);
    display: inline;
  }
</style>