clc
clear all
format short

% To Sove tha LPP by Simplex Method

% Min z = x1-3x2+2x3
% Subject to
% 3x1-x2+2x3<=7
% -2x1+4x2<=12
% -4x1+3x2+8x3<=10
% x1,x2,x3>=0

% First to change the obejective function from minimization to maximization
% Convete to Max z = -x1+3x2-2x3

C = [-1 3 -2 ];

info = [3 -1 2;-2 4 0;-4 3 8];

b = [7;12;10];

NOVariables = size(info,2);

% Add slack variables

s = eye(size(info,1));

A =[info s b];

% Cost row

Cost = zeros(1,size(A,2));
Cost(1:NOVariables) = C;

% Initial Basic Variables
BV = NOVariables+1 : size(A,2)-1;

% Compute Zj -Cj
ZRow = Cost(BV)*A - Cost;

% Display initial table
ZjCj = [ZRow; A];
SimpTable = array2table(ZjCj);
SimpTable.Properties.VariableNames = {'x_1','x_2','x_3','s_1','s_2','s_3','Sol'};

Run = true;

while Run
    if any (ZRow(1,1:end-1)<0)
        
        fprintf('\nThe current BFS is not optimal\n')
        disp('Old Basic Variables (BV)= ')
        disp(BV)
        
        % Entering Variable
        ZC = ZRow(1:end-1);
        [EnterCol,Pvt_Col] = min(ZC) % Most negative element is entered
        
        fprintf('Entering Variable is column %d\n', Pvt_Col)
        
        %Leaving Variable
        sol = A(:,end)
        Column = A(:, Pvt_Col)
        
        if all(Column <= 0)
            error('LPP has unbounded solution ')
        end
        
        ratio = inf(size(Column)); % Give column matrix all entries infinity
        for i = 1:length(Column)
            if Column(i) > 0
                ratio(i) = sol(i) / Column(i);
            end
        end
        
        [MinRatio, Pvt_Row] = min(ratio);
        
        fprintf('Leaving Variables is %d\n', BV(Pvt_Row))
        
        % Update BV
        BV(Pvt_Row) = Pvt_Col; % Replaced leaving variables with entering variables
        disp('New Basic Variables (BV) = ')
        disp(BV)
        
        % Pivot operation
        Pvt_Key = A(Pvt_Row, Pvt_Col);
        A(Pvt_Row,:) = A(Pvt_Row,:) / Pvt_Key; %operation on pivot row
        
        for i = 1:size(A,1)
            if i ~= Pvt_Row
                A(i,:) = A(i,:) - A(i,Pvt_Col)*A(Pvt_Row,:); %operation on rows other than pivot row
            end
        end
        
        % Update Z-row (OUTSIDE the loop!)
        ZRow = ZRow - ZRow(Pvt_Col)*A(Pvt_Row,:)
        
        % Display new table
        ZjCj = [ZRow; A];
        SimpTable = array2table(ZjCj);
        SimpTable.Properties.VariableNames={'x_1','x_2','x_3','s_1','s_2','s_3','Sol'}
        
        disp(SimpTable)
        
        % Current BFS
        
        BFS = zeros(1,size(A,2));
        BFS(BV) = A(:,end);
        BFS(end) = sum(BFS .* Cost);
        CurrentBFS = array2table(BFS);
        CurrentBFS.Properties.VariableNames = {'x_1','x_2','x_3','s_1','s_2','s_3','Sol'}
        
        disp('Current BFS:')
        disp(CurrentBFS)
        
    else
        Run = false;
        fprintf('\n======================================\n')
        fprintf('Optimal solution reached\n')
        fprintf('======================================\n')
    end
end
