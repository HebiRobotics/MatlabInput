classdef (Sealed) HebiKeyboard < handle
  
    properties (SetAccess = private)
        Name
    end
    
    properties (Access = private)
        obj
    end
    
    methods (Static, Access = public)
        function loadLibs()
            % Loads the backing Java files and native binaries. This
            % method assumes that the jar file is located in the same
            % directory as this class-script, and that the file name
            % matches the string below.
            jarFileName = '%RELEASE_NAME%.jar';
            
            % Load only once
            if ~exist('us.hebi.matlab.input.HebiKeyboard', 'class')
                
                localDir = fileparts(mfilename('fullpath'));
                
                % Add binary libs
                java.lang.System.setProperty(...
                    'net.java.games.input.librarypath', ...
                    fullfile(localDir, 'lib'));
                
                % Add Java library
                javaaddpath(fullfile(localDir, jarFileName));
                
            end
        end
    end
    
    methods (Access = public)
        
        function this = HebiKeyboard(driver, index)

            if nargin < 1
                driver = 'AWT';
            end

            if nargin < 2
                index = 1;
            end
           
            % Create backing Java object
            HebiKeyboard.loadLibs();
            this.obj = us.hebi.matlab.input.HebiKeyboard(index, driver);
            if ~ismac()
                % Increase event queue to not have to poll as often.
                % Doesn't work on mac.
                this.obj.setEventQueueSize(200);
            end
            
            % Set properties
            this.Name = this.obj.getName();

        end
        
        function out = read(this)
            out = struct(read(this.obj));
        end

        function [] = close(this)
            % closes and invalidates the keyboard object
            close(this.obj);
        end
        
    end
    
    % Hide inherited methods (handle) from auto-complete
    % and docs
    methods(Access = public, Hidden = true)
        
        function [] = delete(this)
            % destructor disposes this instance
            close(this);
        end
        
        function varargout = addlistener(varargin)
            varargout{:} = addlistener@handle(varargin{:});
        end
        function varargout = eq(varargin)
            varargout{:} = eq@handle(varargin{:});
        end
        function varargout = findobj(varargin)
            varargout{:} = findobj@handle(varargin{:});
        end
        function varargout = findprop(varargin)
            varargout{:} = findprop@handle(varargin{:});
        end
        function varargout = ge(varargin)
            varargout{:} = ge@handle(varargin{:});
        end
        function varargout = gt(varargin)
            varargout{:} = gt@handle(varargin{:});
        end
        function varargout = le(varargin)
            varargout{:} = le@handle(varargin{:});
        end
        function varargout = lt(varargin)
            varargout{:} = lt@handle(varargin{:});
        end
        function varargout = ne(varargin)
            varargout{:} = ne@handle(varargin{:});
        end
        function varargout = notify(varargin)
            varargout{:} = notify@handle(varargin{:});
        end
        
    end
    
end