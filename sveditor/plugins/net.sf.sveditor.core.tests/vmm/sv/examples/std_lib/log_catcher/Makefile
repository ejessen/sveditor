##
## -------------------------------------------------------------
##    Copyright 2004-2008 Synopsys, Inc.
##    All Rights Reserved Worldwide
##
##    Licensed under the Apache License, Version 2.0 (the
##    "License"); you may not use this file except in
##    compliance with the License.  You may obtain a copy of
##    the License at
##
##        http://www.apache.org/licenses/LICENSE-2.0
##
##    Unless required by applicable law or agreed to in
##    writing, software distributed under the License is
##    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
##    CONDITIONS OF ANY KIND, either express or implied.  See
##    the License for the specific language governing
##    permissions and limitations under the License.
## -------------------------------------------------------------
##

OPTS	=

VCS	= vcs -R -sverilog -ntb_opts dtm \
	      +incdir+$(VMM_HOME)/sv+../vmm_test/verif

all default: clean test

test:
	$(VCS) $(OPTS) +define+ALU_BUG1 alu_test.sv \
		../vmm_test/rtl/alu.v ../vmm_test/verif/alu_tb_top.sv

clean:
	rm -rf simv* csrc
	rm -rf vc_hdrs.h .vcsmx_rebuild *.log
	rm -rf work/* *.svshell

