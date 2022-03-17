package com.example.ravn_challenge_v3_fabrizioflorespari.network

import com.apollographql.apollo.ApolloClient

val apolloClient: ApolloClient = ApolloClient.builder()
    .serverUrl("https://swapi-graphql.netlify.app/.netlify/functions/index")
    .build()