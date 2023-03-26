import json

import streamlit as st
from streamlit_extras.switch_page_button import switch_page

from heuristic import Heuristic
from heuristic.data import Parameters

st.set_page_config(page_title="Solver", page_icon=":bar_chart:")
hide_streamlit_style = """
            <style>
            #MainMenu {visibility: hidden;}
            footer {visibility: hidden;}
            </style>
            """
st.markdown(hide_streamlit_style, unsafe_allow_html=True)

def main_page():
    st.title("Solver")
    st.write("**This is a solver for the job shop scheduling problem.**")

if __name__ == "__main__":
    main_page()

    scenario_file = st.file_uploader("**Upload scenario file**", type=["json"])
    button = st.button("Go to solver")
    if button:
        if (scenario_file is None):
            st.error("Please upload a scenario file.")
        else:
            scenario = json.load(scenario_file)

            parameters = Parameters()
            parameters.read_data(json_file=scenario)
            heuristic = Heuristic(parameters)

            st.session_state.scenario = scenario
            st.session_state.parameters = parameters
            st.session_state.heuristic = heuristic
            switch_page("solver_page")
